package com.mycompany.tablemaster.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.entity.DeadLetterMessage;
import com.mycompany.tablemaster.entity.DeadLetterMessageStatus;
import com.mycompany.tablemaster.repository.DeadLetterMessageRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 두 경로의 실패 메시지를 dead_letter_messages 테이블로 영속화한다.
 * <ul>
 *   <li>{@code dead.letter.queue} : 컨슈머 channel 끊김 등 broker 가 직접 dead-letter 한 메시지 (x-death 헤더)</li>
 *   <li>{@code parking.lot.queue} : 인앱 재시도 소진 후 ParkingLotPublisher 가 publish 한 메시지 (x-original-* 헤더)</li>
 * </ul>
 * 자체 처리 실패 시 requeue=false 로 NACK 하여 무한 루프 방지.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DeadLetterConsumer {

    private final DeadLetterMessageRepository deadLetterMessageRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.DLQ)
    public void handleBrokerDeadLetter(Message message, Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        persistAndAck(message, channel, deliveryTag, "broker-dead-letter");
    }

    @RabbitListener(queues = RabbitMQConfig.PARKING_LOT_QUEUE)
    public void handleAppParked(Message message, Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        persistAndAck(message, channel, deliveryTag, "app-retry-exhausted");
    }

    private void persistAndAck(Message message, Channel channel, long deliveryTag, String defaultReason) throws IOException {
        try {
            MessageProperties props = message.getMessageProperties();
            Map<String, Object> headers = props.getHeaders() != null ? props.getHeaders() : Collections.emptyMap();

            FailureMeta meta = extractMeta(headers, defaultReason);
            String payload = decodePayload(message);
            String headersJson = serializeHeaders(headers);

            DeadLetterMessage entity = DeadLetterMessage.builder()
                    .originalExchange(meta.exchange)
                    .originalRoutingKey(meta.routingKey)
                    .originalQueue(meta.queue)
                    .deathReason(meta.reason)
                    .deathCount(meta.count)
                    .firstFailedAt(meta.time)
                    .messageId(props.getMessageId())
                    .contentType(props.getContentType())
                    .headers(headersJson)
                    .payload(payload)
                    .status(DeadLetterMessageStatus.PENDING)
                    .build();

            deadLetterMessageRepository.save(entity);

            log.warn("Dead-letter persisted: id={}, queue={}, reason={}, count={}, source={}",
                    entity.getId(), meta.queue, meta.reason, meta.count, defaultReason);

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to persist dead-letter message (source={})", defaultReason, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @SuppressWarnings("unchecked")
    private FailureMeta extractMeta(Map<String, Object> headers, String defaultReason) {
        FailureMeta meta = new FailureMeta();

        // 1. broker-level: x-death 헤더
        Object xDeath = headers.get("x-death");
        if (xDeath instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
            Map<String, Object> first = (Map<String, Object>) list.get(0);
            meta.queue = asString(first.get("queue"));
            meta.exchange = asString(first.get("exchange"));
            meta.reason = asString(first.get("reason"));
            meta.count = asLong(first.get("count"));
            meta.time = toLocalDateTime(first.get("time"));
            List<?> rks = first.get("routing-keys") instanceof List<?> l ? l : Collections.emptyList();
            if (!rks.isEmpty()) {
                meta.routingKey = asString(rks.get(0));
            }
        }

        // 2. app-level: ParkingLotPublisher 가 부여한 헤더
        if (meta.queue == null) {
            meta.queue = asString(headers.get(RabbitMQConfig.HDR_ORIGINAL_QUEUE));
        }
        if (meta.exchange == null) {
            meta.exchange = asString(headers.get(RabbitMQConfig.HDR_ORIGINAL_EXCHANGE));
        }
        if (meta.routingKey == null) {
            meta.routingKey = asString(headers.get(RabbitMQConfig.HDR_ORIGINAL_ROUTING_KEY));
        }
        if (meta.reason == null) {
            String appReason = asString(headers.get(RabbitMQConfig.HDR_FAILURE_REASON));
            meta.reason = appReason != null ? appReason : defaultReason;
        }
        if (meta.count == null) {
            meta.count = asLong(headers.get(RabbitMQConfig.HDR_APP_RETRY_COUNT));
        }

        // 3. 호환성 보강
        if (meta.queue == null) {
            meta.queue = asString(headers.get("x-first-death-queue"));
        }
        if (meta.exchange == null) {
            meta.exchange = asString(headers.get("x-first-death-exchange"));
        }
        if (meta.reason == null) {
            meta.reason = asString(headers.get("x-first-death-reason"));
            if (meta.reason == null) {
                meta.reason = defaultReason;
            }
        }
        return meta;
    }

    private String decodePayload(Message message) {
        byte[] body = message.getBody();
        if (body == null || body.length == 0) {
            return null;
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private String serializeHeaders(Map<String, Object> headers) {
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize DLQ headers as JSON: {}", e.getMessage());
            return headers.toString();
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private Long asLong(Object value) {
        if (value instanceof Number num) {
            return num.longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        if (value instanceof Number num) {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(num.longValue()), ZoneId.systemDefault());
        }
        return null;
    }

    private static class FailureMeta {
        String queue;
        String exchange;
        String routingKey;
        String reason;
        Long count;
        LocalDateTime time;
    }
}
