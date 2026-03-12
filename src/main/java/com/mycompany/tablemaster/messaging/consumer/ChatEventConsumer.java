package com.mycompany.tablemaster.messaging.consumer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.entity.ChatMessage;
import com.mycompany.tablemaster.entity.ChatRoom;
import com.mycompany.tablemaster.entity.ChatRoomStatus;
import com.mycompany.tablemaster.event.ChatEvent;
import com.mycompany.tablemaster.repository.ChatRoomRepository;
import com.mycompany.tablemaster.service.ChatMessageService;
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
    private final ChatMessageService chatMessageService;
    private final ChatRoomRepository chatRoomRepository;

    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_QUEUE)
    public void handleChatMessage(ChatEvent event, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            // 1. 디바이스 브로드캐스트 (기존 동작 유지)
            Map<String, Object> devicePayload = buildDevicePayload(event);
            webSocketSenderService.broadcast("chat.room." + event.roomId(), devicePayload);

            // 2. DB 저장 (신규)
            ChatRoom chatRoom = chatRoomRepository.findById(event.roomId()).orElse(null);
            if (chatRoom != null && chatRoom.getStatus() == ChatRoomStatus.ACTIVE) {
                ChatMessage savedMessage = chatMessageService.saveMessage(chatRoom, event);
                chatRoomRepository.save(chatRoom);

                // 3. 스태프 모니터 - 목록 갱신용
                webSocketSenderService.broadcast("staff.chat.monitor", Map.of(
                        "type", "ROOM_UPDATED",
                        "roomId", event.roomId(),
                        "totalMessageCount", chatRoom.getTotalMessageCount(),
                        "giftCount", chatRoom.getGiftCount()
                ));

                // 4. 스태프 모니터 - 선택한 채팅방 실시간 메시지
                Map<String, Object> staffPayload = Map.of(
                        "type", resolveStaffMessageType(event),
                        "messageId", savedMessage.getId(),
                        "roomId", event.roomId(),
                        "senderDeviceId", event.senderDeviceId() != null ? event.senderDeviceId() : "",
                        "senderTableName", event.senderTableName() != null ? event.senderTableName() : "",
                        "content", event.message() != null ? event.message() : "",
                        "timestamp", event.timestamp().toString()
                );
                webSocketSenderService.broadcast("staff.chat.room." + event.roomId(), staffPayload);
            }

            log.info("Chat event processed: room={}, type={}, sender={}",
                    event.roomId(), event.type(), event.senderDeviceId());

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to handle chat event: room={}", event.roomId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private Map<String, Object> buildDevicePayload(ChatEvent event) {
        return Map.of(
                "type", event.type().name(),
                "roomId", event.roomId(),
                "senderDeviceId", event.senderDeviceId() != null ? event.senderDeviceId() : "",
                "senderTableName", event.senderTableName() != null ? event.senderTableName() : "",
                "message", event.message() != null ? event.message() : "",
                "messageType", event.messageType() != null ? event.messageType() : "TEXT",
                "timestamp", event.timestamp().toString()
        );
    }

    private String resolveStaffMessageType(ChatEvent event) {
        if (event.messageType() != null) {
            return event.messageType();
        }
        return event.type().name();
    }
}
