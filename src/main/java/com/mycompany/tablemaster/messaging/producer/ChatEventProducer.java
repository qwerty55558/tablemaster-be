package com.mycompany.tablemaster.messaging.producer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.event.ChatEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendMessage(ChatEvent event) {
        String routingKey = RabbitMQConfig.CHAT_MESSAGE_KEY + "." + event.roomId();
        log.info("Sending chat event to room {}: {}", event.roomId(), event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                routingKey,
                event
        );
    }

    public void sendJoin(ChatEvent event) {
        String routingKey = RabbitMQConfig.CHAT_MESSAGE_KEY + "." + event.roomId();
        log.info("Sending join event to room {}: {}", event.roomId(), event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                routingKey,
                event
        );
    }

    public void sendLeave(ChatEvent event) {
        String routingKey = RabbitMQConfig.CHAT_MESSAGE_KEY + "." + event.roomId();
        log.info("Sending leave event to room {}: {}", event.roomId(), event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                routingKey,
                event
        );
    }
}
