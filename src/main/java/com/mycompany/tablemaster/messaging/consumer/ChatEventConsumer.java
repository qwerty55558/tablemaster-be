package com.mycompany.tablemaster.messaging.consumer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.entity.ChatMessage;
import com.mycompany.tablemaster.entity.ChatRoom;
import com.mycompany.tablemaster.entity.ChatRoomParticipant;
import com.mycompany.tablemaster.entity.ChatRoomStatus;
import com.mycompany.tablemaster.event.ChatEvent;
import com.mycompany.tablemaster.event.ChatEventType;
import com.mycompany.tablemaster.repository.ChatRoomRepository;
import com.mycompany.tablemaster.repository.ChatRoomParticipantRepository;
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
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatEventConsumer {

    private final WebSocketSenderService webSocketSenderService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_QUEUE)
    public void handleChatMessage(ChatEvent event, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            // 1. 디바이스 브로드캐스트
            Map<String, Object> roomPayload = buildRoomPayload(event);
            webSocketSenderService.broadcast("chat.room." + event.roomId(), roomPayload);

            // 1-1. 선물은 송신/수신 디바이스별로 다른 이벤트도 전송
            if (event.type() == ChatEventType.GIFT) {
                sendGiftEventsToParticipants(event);
            }

            // 2. DB 저장 (신규)
            ChatRoom chatRoom = chatRoomRepository.findById(event.roomId()).orElse(null);
            if (chatRoom != null && chatRoom.getStatus() == ChatRoomStatus.ACTIVE) {
                ChatMessage savedMessage = chatMessageService.saveMessage(chatRoom, event);
                ChatRoom refreshedRoom = chatRoomRepository.findById(event.roomId()).orElse(chatRoom);

                // 3. 스태프 모니터 - 새 메시지 알림
                webSocketSenderService.broadcast("staff.chat.monitor", Map.of(
                        "type", "CHAT_NEW_MESSAGE",
                        "roomId", event.roomId(),
                        "messageId", savedMessage.getId(),
                        "senderDeviceId", event.senderDeviceId() != null ? event.senderDeviceId() : "",
                        "senderTableName", event.senderTableName() != null ? event.senderTableName() : "",
                        "content", event.message() != null ? event.message() : "",
                        "messageType", event.messageType() != null ? event.messageType() : "TEXT",
                        "createdAt", savedMessage.getCreatedAt().toString(),
                        "totalMessageCount", refreshedRoom.getTotalMessageCount(),
                        "giftCount", refreshedRoom.getGiftCount()
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

    private Map<String, Object> buildRoomPayload(ChatEvent event) {
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

    private void sendGiftEventsToParticipants(ChatEvent event) {
        List<ChatRoomParticipant> participants = chatRoomParticipantRepository.findByChatRoomId(event.roomId());
        for (ChatRoomParticipant participant : participants) {
            boolean isSender = participant.getDeviceId().equals(event.senderDeviceId());
            webSocketSenderService.sendChatToDevice(participant.getDeviceId(), Map.of(
                    "type", isSender ? "CHAT_GIFT_SENT" : "CHAT_GIFT_RECEIVED",
                    "eventType", ChatEventType.GIFT.name(),
                    "roomId", event.roomId(),
                    "senderDeviceId", event.senderDeviceId() != null ? event.senderDeviceId() : "",
                    "senderTableName", event.senderTableName() != null ? event.senderTableName() : "",
                    "giftType", event.message() != null ? event.message() : "",
                    "messageType", "GIFT",
                    "timestamp", event.timestamp().toString()
            ));
        }
    }

    private String resolveStaffMessageType(ChatEvent event) {
        if (event.messageType() != null) {
            return event.messageType();
        }
        return event.type().name();
    }
}
