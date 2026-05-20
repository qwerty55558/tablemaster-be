package com.mycompany.tablemaster.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.entity.DeadLetterMessage;
import com.mycompany.tablemaster.entity.DeadLetterMessageStatus;
import com.mycompany.tablemaster.repository.DeadLetterMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 인앱 재시도가 소진된 메시지를 파킹랏 큐로 publish.
 * publisher confirm 으로 broker 영속화를 확인한 후 ACK 신호를 보낼 수 있게 boolean 반환.
 * confirm 실패/타임아웃 시 DB(dead_letter_messages) 에 직접 INSERT 하여 메시지 유실 방지.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ParkingLotPublisher {

    private static final long CONFIRM_TIMEOUT_MS = 5_000L;

    private final RabbitTemplate rabbitTemplate;
    private final DeadLetterMessageRepository deadLetterMessageRepository;
    private final ObjectMapper objectMapper;

    /**
     * 파킹랏으로 메시지를 안전하게 이관. 절대 false 를 반환하지 않고, broker publish 실패 시에도
     * DB 직접 적재로 대체하여 호출자는 ACK 가능. 두 경로 모두 실패하면 RuntimeException → 호출자는 NACK.
     */
    public void parkOrFallback(Message originalMessage,
                                String originalQueue,
                                int retryAttempts,
                                String failureReason) {
        Message parkingMessage = wrap(originalMessage, originalQueue, retryAttempts, failureReason);
        try {
            publishWithConfirm(parkingMessage);
            log.warn("Parked to broker: queue={}, reason={}, attempts={}",
                    originalQueue, failureReason, retryAttempts);
            return;
        } catch (Exception e) {
            log.error("Parking lot publish failed, falling back to DB: queue={}, cause={}",
                    originalQueue, e.toString());
        }
        persistToDbFallback(originalMessage, originalQueue, retryAttempts, failureReason);
    }

    private void publishWithConfirm(Message parkingMessage) throws InterruptedException, TimeoutException {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.send(RabbitMQConfig.PARKING_EXCHANGE,
                RabbitMQConfig.PARKING_ROUTING_KEY,
                parkingMessage,
                correlationData);

        CorrelationData.Confirm confirm;
        try {
            confirm = correlationData.getFuture().get(CONFIRM_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("Confirm future failed", e.getCause());
        }
        if (confirm == null) {
            throw new TimeoutException("Publisher confirm null");
        }
        if (!confirm.ack()) {
            throw new IllegalStateException("Publisher confirm NACK: " + confirm.reason());
        }
        // mandatory=true 일 때 라우팅 실패 시 return 이 confirm 보다 먼저 도착함
        if (correlationData.getReturned() != null) {
            throw new IllegalStateException("Parking message returned (unroutable): "
                    + correlationData.getReturned().getReplyText());
        }
    }

    private Message wrap(Message original, String originalQueue, int retryAttempts, String failureReason) {
        MessageProperties orig = original.getMessageProperties();
        MessageProperties props = new MessageProperties();
        props.setContentType(orig.getContentType());
        props.setContentEncoding(orig.getContentEncoding());
        props.setMessageId(orig.getMessageId());
        props.setDeliveryMode(orig.getReceivedDeliveryMode() != null
                ? orig.getReceivedDeliveryMode()
                : org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
        if (orig.getHeaders() != null) {
            orig.getHeaders().forEach(props::setHeader);
        }
        props.setHeader(RabbitMQConfig.HDR_ORIGINAL_EXCHANGE, orig.getReceivedExchange());
        props.setHeader(RabbitMQConfig.HDR_ORIGINAL_ROUTING_KEY, orig.getReceivedRoutingKey());
        props.setHeader(RabbitMQConfig.HDR_ORIGINAL_QUEUE, originalQueue);
        props.setHeader(RabbitMQConfig.HDR_FAILURE_REASON, truncate(failureReason, 500));
        props.setHeader(RabbitMQConfig.HDR_APP_RETRY_COUNT, retryAttempts);
        props.setHeader(RabbitMQConfig.HDR_APP_PARKED_AT, LocalDateTime.now().toString());
        return MessageBuilder.withBody(original.getBody()).andProperties(props).build();
    }

    private void persistToDbFallback(Message original, String originalQueue, int retryAttempts, String failureReason) {
        try {
            Map<String, Object> headerMap = new HashMap<>();
            if (original.getMessageProperties().getHeaders() != null) {
                headerMap.putAll(original.getMessageProperties().getHeaders());
            }
            headerMap.put(RabbitMQConfig.HDR_APP_RETRY_COUNT, retryAttempts);
            headerMap.put(RabbitMQConfig.HDR_FAILURE_REASON, failureReason);
            String headerJson;
            try {
                headerJson = objectMapper.writeValueAsString(headerMap);
            } catch (Exception ignored) {
                headerJson = headerMap.toString();
            }

            byte[] body = original.getBody();
            String payload = (body == null || body.length == 0)
                    ? null
                    : new String(body, StandardCharsets.UTF_8);

            DeadLetterMessage entity = DeadLetterMessage.builder()
                    .originalExchange(original.getMessageProperties().getReceivedExchange())
                    .originalRoutingKey(original.getMessageProperties().getReceivedRoutingKey())
                    .originalQueue(originalQueue)
                    .deathReason("app-retry-exhausted-broker-unavailable")
                    .deathCount((long) retryAttempts)
                    .firstFailedAt(LocalDateTime.now())
                    .messageId(original.getMessageProperties().getMessageId())
                    .contentType(original.getMessageProperties().getContentType())
                    .headers(headerJson)
                    .payload(payload)
                    .status(DeadLetterMessageStatus.PENDING)
                    .build();

            deadLetterMessageRepository.save(entity);
            log.warn("Parked via DB fallback: id={}, queue={}, attempts={}",
                    entity.getId(), originalQueue, retryAttempts);
        } catch (Exception dbError) {
            log.error("DB fallback also failed for queue={}, attempts={}", originalQueue, retryAttempts, dbError);
            throw new IllegalStateException("Both broker and DB parking failed", dbError);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
