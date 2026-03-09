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
    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String DEVICE_EXCHANGE = "device.exchange";
    public static final String DLX_EXCHANGE = "dlx.exchange";

    // ============== Queue Names ==============
    public static final String CHAT_MESSAGE_QUEUE = "chat.message.queue";
    public static final String NOTIFICATION_INAPP_QUEUE = "notification.inapp.queue";
    public static final String DEVICE_EVENT_QUEUE = "device.event.queue";
    public static final String DLQ = "dead.letter.queue";

    // ============== Routing Keys ==============
    public static final String CHAT_MESSAGE_KEY = "chat.message";
    public static final String DEVICE_EVENT_KEY = "device.event";

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

    // ============== Queues ==============

    @Bean
    public Queue chatMessageQueue() {
        return QueueBuilder.durable(CHAT_MESSAGE_QUEUE)
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
    public Queue deviceEventQueue() {
        return QueueBuilder.durable(DEVICE_EVENT_QUEUE)
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
