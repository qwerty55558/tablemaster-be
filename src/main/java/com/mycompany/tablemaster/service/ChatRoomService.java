package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.dto.table.TableListResponse;
import com.mycompany.tablemaster.entity.*;
import com.mycompany.tablemaster.event.ChatEvent;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.messaging.producer.ChatEventProducer;
import com.mycompany.tablemaster.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatReportRepository chatReportRepository;
    private final ChatRoomHistoryRepository chatRoomHistoryRepository;
    private final StaffChatReadPositionRepository staffChatReadPositionRepository;
    private final TableRepository tableRepository;
    private final ChatMessageService chatMessageService;
    private final WebSocketSenderService webSocketSenderService;
    private final ChatEventProducer chatEventProducer;

    @Transactional
    public ChatRoom createRoom(String deviceId1, String tableName1,
                                String deviceId2, String tableName2) {
        ChatRoom chatRoom = ChatRoom.builder()
                .status(ChatRoomStatus.ACTIVE)
                .build();
        chatRoomRepository.save(chatRoom);

        ChatRoomParticipant p1 = ChatRoomParticipant.builder()
                .chatRoom(chatRoom)
                .deviceId(deviceId1)
                .tableName(tableName1)
                .build();

        ChatRoomParticipant p2 = ChatRoomParticipant.builder()
                .chatRoom(chatRoom)
                .deviceId(deviceId2)
                .tableName(tableName2)
                .build();

        chatRoomParticipantRepository.save(p1);
        chatRoomParticipantRepository.save(p2);

        // 테이블 상태 변경
        tableRepository.findById(deviceId1).ifPresent(TableEntity::startChatting);
        tableRepository.findById(deviceId2).ifPresent(TableEntity::startChatting);

        // 스태프 모니터에 알림
        webSocketSenderService.broadcast("staff.chat.monitor", Map.of(
                "type", "ROOM_CREATED",
                "roomId", chatRoom.getId(),
                "participants", List.of(
                        Map.of("deviceId", deviceId1, "tableName", tableName1),
                        Map.of("deviceId", deviceId2, "tableName", tableName2)
                )
        ));

        log.info("Chat room created: roomId={}, {} ↔ {}", chatRoom.getId(), tableName1, tableName2);
        return chatRoom;
    }

    @Transactional
    public void closeRoom(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(BusinessException::chatRoomNotFound);

        if (chatRoom.getStatus() != ChatRoomStatus.ACTIVE) {
            return;
        }

        chatRoom.close();

        // 참여자 테이블 상태 복원 (다른 활성 채팅방이 없는 경우만)
        List<ChatRoomParticipant> participants = chatRoomParticipantRepository.findByChatRoomId(roomId);
        for (ChatRoomParticipant p : participants) {
            if (chatRoomParticipantRepository.countActiveRoomsByDeviceId(p.getDeviceId()) == 0) {
                tableRepository.findById(p.getDeviceId()).ifPresent(table -> {
                    table.endChatting();
                    TableEntity saved = tableRepository.save(table);
                    broadcastTableUpdated(saved);
                });
            }
        }

        // 스태프 모니터에 알림
        webSocketSenderService.broadcast("staff.chat.monitor", Map.of(
                "type", "ROOM_CLOSED",
                "roomId", roomId
        ));

        log.info("Chat room closed: roomId={}", roomId);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getActiveRooms() {
        return chatRoomRepository.findByStatusOrderByStartedAtDesc(ChatRoomStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getAllRooms() {
        return chatRoomRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ChatRoom getRoomDetail(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(BusinessException::chatRoomNotFound);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomParticipant> getParticipants(Long roomId) {
        return chatRoomParticipantRepository.findByChatRoomId(roomId);
    }

    @Transactional
    public void sanctionRoom(Long roomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(BusinessException::chatRoomNotFound);

        chatRoom.sanction();

        // 시스템 메시지 저장
        chatMessageService.saveSystemMessage(chatRoom, "관리자에 의해 채팅이 제재되었습니다.");

        // 참여자에게 채팅 종료 알림 + 테이블 상태 복원
        List<ChatRoomParticipant> participants = chatRoomParticipantRepository.findByChatRoomId(roomId);
        for (ChatRoomParticipant p : participants) {
            if (chatRoomParticipantRepository.countActiveRoomsByDeviceId(p.getDeviceId()) == 0) {
                tableRepository.findById(p.getDeviceId()).ifPresent(table -> {
                    table.endChatting();
                    TableEntity saved = tableRepository.save(table);
                    broadcastTableUpdated(saved);
                });
            }

            webSocketSenderService.sendChatToDevice(p.getDeviceId(), Map.of(
                    "type", "CHAT_SANCTIONED",
                    "roomId", roomId
            ));
        }

        // 스태프 모니터에 알림
        webSocketSenderService.broadcast("staff.chat.monitor", Map.of(
                "type", "ROOM_SANCTIONED",
                "roomId", roomId,
                "sanctionedBy", userId
        ));

        log.info("Chat room sanctioned: roomId={}, by userId={}", roomId, userId);
    }

    @Transactional
    public boolean toggleMute(Long roomId, String deviceId) {
        ChatRoomParticipant participant = chatRoomParticipantRepository
                .findByChatRoomIdAndDeviceId(roomId, deviceId)
                .orElseThrow(BusinessException::chatParticipantNotFound);

        participant.setIsMuted(!participant.getIsMuted());
        chatRoomParticipantRepository.save(participant);

        log.info("Mute toggled: roomId={}, deviceId={}, isMuted={}", roomId, deviceId, participant.getIsMuted());
        return participant.getIsMuted();
    }

    @Transactional(readOnly = true)
    public boolean isMuted(Long roomId, String deviceId) {
        return chatRoomParticipantRepository
                .findByChatRoomIdAndDeviceId(roomId, deviceId)
                .map(ChatRoomParticipant::getIsMuted)
                .orElse(false);
    }

    /**
     * 디바이스 삭제 시 관련 채팅 데이터 로그 후 삭제
     */
    @Transactional
    public void cleanupByDeviceId(String deviceId) {
        List<ChatRoomParticipant> participations = chatRoomParticipantRepository.findByDeviceId(deviceId);
        if (participations.isEmpty()) {
            return;
        }

        Set<Long> roomIds = participations.stream()
                .map(p -> p.getChatRoom().getId())
                .collect(Collectors.toSet());

        for (Long roomId : roomIds) {
            logAndDeleteRoom(roomId, deviceId, "DEVICE_DELETED");
        }

        log.info("Chat cleanup completed: deviceId={}, rooms={}", deviceId, roomIds.size());
    }

    private void logAndDeleteRoom(Long roomId, String triggerDeviceId, String deleteReason) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return;
        }

        List<ChatRoomParticipant> participants = chatRoomParticipantRepository.findByChatRoomId(roomId);

        // 히스토리 저장
        String participantNames = participants.stream()
                .map(p -> p.getTableName() + "(" + p.getDeviceId() + ")")
                .collect(Collectors.joining(" ↔ "));

        chatRoomHistoryRepository.save(ChatRoomHistory.builder()
                .roomId(roomId)
                .participants(participantNames)
                .status(room.getStatus())
                .totalMessageCount(room.getTotalMessageCount())
                .giftCount(room.getGiftCount())
                .reportCount(room.getReportCount())
                .startedAt(room.getStartedAt())
                .closedAt(room.getClosedAt())
                .deleteReason(deleteReason)
                .build());

        // 참여자 테이블 상태 복원 (삭제 트리거 디바이스 제외)
        if (room.getStatus() == ChatRoomStatus.ACTIVE) {
            for (ChatRoomParticipant p : participants) {
                if (!p.getDeviceId().equals(triggerDeviceId)) {
                    webSocketSenderService.sendChatToDevice(p.getDeviceId(), Map.of(
                            "type", "CHAT_CLOSED",
                            "roomId", roomId,
                            "reason", "PARTICIPANT_DELETED"
                    ));
                    if (chatRoomParticipantRepository.countActiveRoomsByDeviceId(p.getDeviceId()) <= 1) {
                        tableRepository.findById(p.getDeviceId()).ifPresent(table -> {
                            table.endChatting();
                            TableEntity saved = tableRepository.save(table);
                            broadcastTableUpdated(saved);
                        });
                    }
                }
            }
        }

        // 삭제 순서: 메시지 → 신고 → 읽기위치 → 참여자 → 방
        chatMessageRepository.deleteByChatRoomId(roomId);
        chatReportRepository.deleteByChatRoomId(roomId);
        staffChatReadPositionRepository.deleteByChatRoomId(roomId);
        chatRoomParticipantRepository.deleteByChatRoomId(roomId);
        chatRoomRepository.delete(room);

        log.info("Chat room deleted and archived: roomId={}, reason={}", roomId, deleteReason);
    }

    private void broadcastTableUpdated(TableEntity table) {
        webSocketSenderService.broadcast("tables", Map.of(
                "type", "TABLE_UPDATED",
                "data", TableListResponse.from(table),
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}
