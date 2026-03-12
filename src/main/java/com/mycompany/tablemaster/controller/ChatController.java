package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.chat.*;
import com.mycompany.tablemaster.entity.ChatRoom;
import com.mycompany.tablemaster.entity.TableEntity;
import com.mycompany.tablemaster.event.ChatEvent;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.messaging.producer.ChatEventProducer;
import com.mycompany.tablemaster.repository.TableRepository;
import com.mycompany.tablemaster.service.ChatRoomService;
import com.mycompany.tablemaster.service.WebSocketSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final ChatEventProducer chatEventProducer;
    private final TableRepository tableRepository;
    private final WebSocketSenderService webSocketSenderService;

    /**
     * 채팅 요청
     * 클라이언트: /app/chat/request
     */
    @MessageMapping("/chat/request")
    public void handleChatRequest(ChatRequestMessage message, Principal principal) {
        String senderDeviceId = principal.getName();
        String targetDeviceId = message.getTargetTableId();

        TableEntity senderTable = tableRepository.findById(senderDeviceId)
                .orElseThrow(BusinessException::tableNotFound);
        TableEntity targetTable = tableRepository.findById(targetDeviceId)
                .orElseThrow(BusinessException::tableNotFound);

        if (!targetTable.getIsChatEnabled()) {
            webSocketSenderService.sendChatToDevice(senderDeviceId, Map.of(
                    "type", "CHAT_REQUEST_FAILED",
                    "reason", "상대 테이블이 채팅을 허용하지 않았습니다"
            ));
            return;
        }

        // 상대방에게 채팅 요청 전송
        webSocketSenderService.sendChatToDevice(targetDeviceId, Map.of(
                "type", "CHAT_REQUEST",
                "fromDeviceId", senderDeviceId,
                "fromTableName", senderTable.getName()
        ));

        log.info("Chat request sent: {} → {}", senderTable.getName(), targetTable.getName());
    }

    /**
     * 채팅 수락
     * 클라이언트: /app/chat/accept
     */
    @MessageMapping("/chat/accept")
    public void handleChatAccept(ChatAcceptMessage message, Principal principal) {
        String acceptorDeviceId = principal.getName();
        String requesterDeviceId = message.getTargetTableId();

        TableEntity acceptorTable = tableRepository.findById(acceptorDeviceId)
                .orElseThrow(BusinessException::tableNotFound);
        TableEntity requesterTable = tableRepository.findById(requesterDeviceId)
                .orElseThrow(BusinessException::tableNotFound);

        // 채팅방 생성
        ChatRoom chatRoom = chatRoomService.createRoom(
                requesterDeviceId, requesterTable.getName(),
                acceptorDeviceId, acceptorTable.getName()
        );

        // 양쪽에 채팅 수락 알림
        Map<String, Object> acceptPayload = Map.of(
                "type", "CHAT_ACCEPTED",
                "roomId", chatRoom.getId(),
                "partnerDeviceId", acceptorDeviceId,
                "partnerTableName", acceptorTable.getName()
        );
        webSocketSenderService.sendChatToDevice(requesterDeviceId, acceptPayload);

        Map<String, Object> acceptPayload2 = Map.of(
                "type", "CHAT_ACCEPTED",
                "roomId", chatRoom.getId(),
                "partnerDeviceId", requesterDeviceId,
                "partnerTableName", requesterTable.getName()
        );
        webSocketSenderService.sendChatToDevice(acceptorDeviceId, acceptPayload2);

        log.info("Chat accepted: roomId={}, {} ↔ {}", chatRoom.getId(), requesterTable.getName(), acceptorTable.getName());
    }

    /**
     * 채팅 거절
     * 클라이언트: /app/chat/reject
     */
    @MessageMapping("/chat/reject")
    public void handleChatReject(ChatRejectMessage message, Principal principal) {
        String rejectorDeviceId = principal.getName();
        String requesterDeviceId = message.getTargetTableId();

        TableEntity rejectorTable = tableRepository.findById(rejectorDeviceId)
                .orElseThrow(BusinessException::tableNotFound);

        webSocketSenderService.sendChatToDevice(requesterDeviceId, Map.of(
                "type", "CHAT_REJECTED",
                "fromDeviceId", rejectorDeviceId,
                "fromTableName", rejectorTable.getName()
        ));

        log.info("Chat rejected: {} rejected by {}", requesterDeviceId, rejectorTable.getName());
    }

    /**
     * 채팅 퇴장
     * 클라이언트: /app/chat/leave
     */
    @MessageMapping("/chat/leave")
    public void handleChatLeave(ChatLeaveMessage message, Principal principal) {
        String deviceId = principal.getName();
        Long roomId = message.getRoomId();

        TableEntity table = tableRepository.findById(deviceId)
                .orElseThrow(BusinessException::tableNotFound);

        // LEAVE 이벤트를 RabbitMQ로 발행 → ChatEventConsumer가 처리
        chatEventProducer.sendLeave(ChatEvent.leave(roomId, deviceId, table.getName()));

        // 채팅방 종료
        chatRoomService.closeRoom(roomId);

        log.info("Chat leave: roomId={}, deviceId={}", roomId, deviceId);
    }

    /**
     * 메시지 전송
     * 클라이언트: /app/chat/send
     */
    @MessageMapping("/chat/send")
    public void handleChatSend(ChatSendMessage message, Principal principal) {
        String deviceId = principal.getName();

        TableEntity table = tableRepository.findById(deviceId)
                .orElseThrow(BusinessException::tableNotFound);

        Long roomId = message.getRoomId();

        // 음소거 확인
        if (chatRoomService.isMuted(roomId, deviceId)) {
            webSocketSenderService.sendChatToDevice(deviceId, Map.of(
                    "type", "CHAT_MUTED",
                    "roomId", roomId
            ));
            return;
        }

        // RabbitMQ로 이벤트 발행
        ChatEvent event = ChatEvent.message(message.getRoomId(), deviceId, table.getName(), message.getContent());
        chatEventProducer.sendMessage(event);
    }

    /**
     * 선물 전송
     * 클라이언트: /app/chat/gift
     */
    @MessageMapping("/chat/gift")
    public void handleChatGift(ChatGiftMessage message, Principal principal) {
        String deviceId = principal.getName();

        TableEntity table = tableRepository.findById(deviceId)
                .orElseThrow(BusinessException::tableNotFound);

        ChatEvent event = ChatEvent.gift(message.getRoomId(), deviceId, table.getName(), message.getGiftType());
        chatEventProducer.sendMessage(event);

        log.info("Gift sent: roomId={}, from={}, type={}", message.getRoomId(), table.getName(), message.getGiftType());
    }

    @MessageExceptionHandler(BusinessException.class)
    public void handleBusinessException(BusinessException ex, Principal principal) {
        webSocketSenderService.sendChatToDevice(principal.getName(), Map.of(
                "type", "CHAT_ERROR",
                "code", ex.getCode(),
                "reason", ex.getMessage()
        ));
    }
}
