package com.mycompany.tablemaster.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQConfig {

    // ============== Exchange Names ==============
    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String DEVICE_EXCHANGE = "device.exchange";
    public static final String DLX_EXCHANGE = "dlx.exchange";
    public static final String PARKING_EXCHANGE = "parking.exchange";

    // ============== Queue Names ==============
    public static final String CHAT_MESSAGE_QUEUE = "chat.message.queue";
    public static final String NOTIFICATION_INAPP_QUEUE = "notification.inapp.queue";
    public static final String DEVICE_EVENT_QUEUE = "device.event.queue";
    public static final String DLQ = "dead.letter.queue";
    public static final String PARKING_LOT_QUEUE = "parking.lot.queue";

    // ============== Routing Keys ==============
    public static final String CHAT_MESSAGE_KEY = "chat.message";
    public static final String DEVICE_EVENT_KEY = "device.event";
    public static final String DLQ_ROUTING_KEY = "dlq";
    public static final String PARKING_ROUTING_KEY = "parking";

    // ============== Headers (app-level) ==============
    public static final String HDR_ORIGINAL_EXCHANGE = "x-original-exchange";
    public static final String HDR_ORIGINAL_ROUTING_KEY = "x-original-routing-key";
    public static final String HDR_ORIGINAL_QUEUE = "x-original-queue";
    public static final String HDR_FAILURE_REASON = "x-failure-reason";
    public static final String HDR_APP_RETRY_COUNT = "x-app-retry-count";
    public static final String HDR_APP_PARKED_AT = "x-app-parked-at";

    // ============== Exchanges ==============

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE);
    }

    @Bean
    public FanoutExchange notificationExchange() {
        return new FanoutExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public DirectExchange deviceExchange() {
        return new DirectExchange(DEVICE_EXCHANGE);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public DirectExchange parkingExchange() {
        return new DirectExchange(PARKING_EXCHANGE);
    }

    // ============== Queues ==============

    @Bean
    public Queue chatMessageQueue() {
        return QueueBuilder.durable(CHAT_MESSAGE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue notificationInAppQueue() {
        return QueueBuilder.durable(NOTIFICATION_INAPP_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deviceEventQueue() {
        return QueueBuilder.durable(DEVICE_EVENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    /** Broker-level 안전망 DLQ (consumer가 죽거나 channel 끊김 등으로 NACK된 메시지). */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    /** 앱 레벨 재시도 소진 메시지가 적재되는 파킹랏 큐 (운영자 개입 대상). */
    @Bean
    public Queue parkingLotQueue() {
        return QueueBuilder.durable(PARKING_LOT_QUEUE).build();
    }

    // ============== Bindings ==============

    @Bean
    public Binding chatMessageBinding() {
        return BindingBuilder.bind(chatMessageQueue())
                .to(chatExchange())
                .with("chat.message.#");
    }

    @Bean
    public Binding notificationInAppBinding() {
        return BindingBuilder.bind(notificationInAppQueue())
                .to(notificationExchange());
    }

    @Bean
    public Binding deviceEventBinding() {
        return BindingBuilder.bind(deviceEventQueue())
                .to(deviceExchange())
                .with(DEVICE_EVENT_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(dlxExchange())
                .with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding parkingLotBinding() {
        return BindingBuilder.bind(parkingLotQueue())
                .to(parkingExchange())
                .with(PARKING_ROUTING_KEY);
    }

    // ============== Message Converter ==============

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    @SuppressWarnings("removal")
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setMandatory(true);

        // Publisher confirm — 메시지가 broker 큐에 영속화됐는지 확인
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("Publisher confirm NACK: correlationData={}, cause={}", correlationData, cause);
            } else if (log.isDebugEnabled() && correlationData != null) {
                log.debug("Publisher confirm ACK: correlationData={}", correlationData.getId());
            }
        });

        // 라우팅 불가 메시지 감지 (mandatory=true 와 함께)
        rabbitTemplate.setReturnsCallback(returned -> log.error(
                "Message returned (unroutable): exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(),
                returned.getReplyCode(), returned.getReplyText()));

        return rabbitTemplate;
    }
}
