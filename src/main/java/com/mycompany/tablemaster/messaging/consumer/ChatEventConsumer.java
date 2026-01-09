package com.mycompany.tablemaster.messaging.consumer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.event.ChatEvent;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class ChatEventConsumer {

    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_QUEUE)
    public void handleChatMessage(ChatEvent event, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            switch (event.type()) {
                case MESSAGE -> {
                    log.info("Chat message received: room={}, sender={}, message={}",
                            event.roomId(), event.senderId(), event.message());
                    // TODO: 채팅 메시지 처리
                    // 1. 메시지 저장
                    // 2. 방 참여자들에게 WebSocket으로 전달
                }
                case JOIN -> {
                    log.info("User joined chat room: room={}, user={}", event.roomId(), event.senderId());
                    // TODO: 입장 알림 처리
                }
                case LEAVE -> {
                    log.info("User left chat room: room={}, user={}", event.roomId(), event.senderId());
                    // TODO: 퇴장 알림 처리
                }
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to handle chat event: room={}", event.roomId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
