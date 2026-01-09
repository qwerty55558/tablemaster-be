package com.mycompany.tablemaster.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ============== Exchange Names ==============
    public static final String GIFT_EXCHANGE = "gift.exchange";
    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String TABLE_EXCHANGE = "table.exchange";
    public static final String DLX_EXCHANGE = "dlx.exchange";

    // ============== Queue Names ==============
    public static final String GIFT_PROCESS_QUEUE = "gift.process.queue";
    public static final String GIFT_COMPLETE_QUEUE = "gift.complete.queue";
    public static final String CHAT_MESSAGE_QUEUE = "chat.message.queue";
    public static final String NOTIFICATION_PUSH_QUEUE = "notification.push.queue";
    public static final String NOTIFICATION_INAPP_QUEUE = "notification.inapp.queue";
    public static final String TABLE_RESET_QUEUE = "table.reset.queue";
    public static final String DLQ = "dead.letter.queue";

    // ============== Routing Keys ==============
    public static final String GIFT_PROCESS_KEY = "gift.process";
    public static final String GIFT_COMPLETE_KEY = "gift.complete";
    public static final String CHAT_MESSAGE_KEY = "chat.message";
    public static final String TABLE_RESET_KEY = "table.reset";

    // ============== Exchanges ==============

    @Bean
    public DirectExchange giftExchange() {
        return new DirectExchange(GIFT_EXCHANGE);
    }

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE);
    }

    @Bean
    public FanoutExchange notificationExchange() {
        return new FanoutExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public DirectExchange tableExchange() {
        return new DirectExchange(TABLE_EXCHANGE);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    // ============== Queues ==============

    @Bean
    public Queue giftProcessQueue() {
        return QueueBuilder.durable(GIFT_PROCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue giftCompleteQueue() {
        return QueueBuilder.durable(GIFT_COMPLETE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue chatMessageQueue() {
        return QueueBuilder.durable(CHAT_MESSAGE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue notificationPushQueue() {
        return QueueBuilder.durable(NOTIFICATION_PUSH_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue notificationInAppQueue() {
        return QueueBuilder.durable(NOTIFICATION_INAPP_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue tableResetQueue() {
        return QueueBuilder.durable(TABLE_RESET_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    // ============== Bindings ==============

    @Bean
    public Binding giftProcessBinding() {
        return BindingBuilder.bind(giftProcessQueue())
                .to(giftExchange())
                .with(GIFT_PROCESS_KEY);
    }

    @Bean
    public Binding giftCompleteBinding() {
        return BindingBuilder.bind(giftCompleteQueue())
                .to(giftExchange())
                .with(GIFT_COMPLETE_KEY);
    }

    @Bean
    public Binding chatMessageBinding() {
        return BindingBuilder.bind(chatMessageQueue())
                .to(chatExchange())
                .with("chat.message.#");
    }

    @Bean
    public Binding notificationPushBinding() {
        return BindingBuilder.bind(notificationPushQueue())
                .to(notificationExchange());
    }

    @Bean
    public Binding notificationInAppBinding() {
        return BindingBuilder.bind(notificationInAppQueue())
                .to(notificationExchange());
    }

    @Bean
    public Binding tableResetBinding() {
        return BindingBuilder.bind(tableResetQueue())
                .to(tableExchange())
                .with(TABLE_RESET_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(dlxExchange())
                .with("dlq");
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
        return rabbitTemplate;
    }
}
