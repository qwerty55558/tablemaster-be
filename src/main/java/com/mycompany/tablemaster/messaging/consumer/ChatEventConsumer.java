package com.mycompany.tablemaster.messaging.consumer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.event.ChatEvent;
import com.mycompany.tablemaster.service.WebSocketSenderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatEventConsumer {

    private final WebSocketSenderService webSocketSenderService;

    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_QUEUE)
    public void handleChatMessage(ChatEvent event, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            switch (event.type()) {
                case MESSAGE -> {
                    log.info("Chat message received: room={}, sender={}, message={}",
                            event.roomId(), event.senderId(), event.message());

                    // 채팅 메시지를 해당 토픽으로 브로드캐스트
                    Map<String, Object> payload = Map.of(
                        "type", "MESSAGE",
                        "roomId", event.roomId(),
                        "senderId", event.senderId(),
                        "message", event.message(),
                        "timestamp", event.timestamp().toString()
                    );
                    webSocketSenderService.broadcast("chat.room." + event.roomId(), payload);
                }
                case JOIN -> {
                    log.info("User joined chat room: room={}, user={}", event.roomId(), event.senderId());

                    Map<String, Object> payload = Map.of(
                        "type", "JOIN",
                        "roomId", event.roomId(),
                        "senderId", event.senderId(),
                        "timestamp", event.timestamp().toString()
                    );
                    webSocketSenderService.broadcast("chat.room." + event.roomId(), payload);
                }
                case LEAVE -> {
                    log.info("User left chat room: room={}, user={}", event.roomId(), event.senderId());

                    Map<String, Object> payload = Map.of(
                        "type", "LEAVE",
                        "roomId", event.roomId(),
                        "senderId", event.senderId(),
                        "timestamp", event.timestamp().toString()
                    );
                    webSocketSenderService.broadcast("chat.room." + event.roomId(), payload);
                }
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to handle chat event: room={}", event.roomId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
