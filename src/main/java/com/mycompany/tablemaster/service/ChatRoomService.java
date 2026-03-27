package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.dto.table.TableListResponse;
import com.mycompany.tablemaster.entity.*;
import com.mycompany.tablemaster.event.ChatEvent;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.messaging.producer.ChatEventProducer;
import com.mycompany.tablemaster.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ChatModerationHistoryService chatModerationHistoryService;
    private final WebSocketSenderService webSocketSenderService;
    private final ChatEventProducer chatEventProducer;
    private final StringRedisTemplate redisTemplate;
    private final EntityManager entityManager;
    private final AnalyticsLogService analyticsLogService;

    @Transactional(readOnly = true)
    public boolean hasActiveRoomBetween(String deviceId1, String deviceId2) {
        return chatRoomParticipantRepository.existsActiveRoomBetween(deviceId1, deviceId2);
    }

    @Transactional(readOnly = true)
    public Optional<ChatRoom> findReusableRoomBetween(String deviceId1, String deviceId2) {
        return chatRoomRepository.findReusableRoomBetween(deviceId1, deviceId2);
    }

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

        // 테이블 상태 변경 + 브로드캐스트 (participant가 아직 flush 안 됐을 수 있으므로 직접 전달)
        tableRepository.findById(deviceId1).ifPresent(table -> {
            table.startChatting();
            TableEntity saved = tableRepository.save(table);
            broadcastTableWithChatInfo(saved, chatRoom.getId(), null, false, null);
        });
        tableRepository.findById(deviceId2).ifPresent(table -> {
            table.startChatting();
            TableEntity saved = tableRepository.save(table);
            broadcastTableWithChatInfo(saved, chatRoom.getId(), null, false, null);
        });

        // 스태프 모니터에 알림 - 프론트에서 목록에 바로 추가할 수 있도록 room 데이터 포함
        webSocketSenderService.broadcast("staff.chat.monitor", Map.of(
                "type", "CHAT_ROOM_CREATED",
                "roomId", chatRoom.getId(),
                "room", Map.of(
                        "id", chatRoom.getId(),
                        "status", chatRoom.getStatus(),
                        "startedAt", chatRoom.getStartedAt().toString(),
                        "totalMessageCount", 0,
                        "giftCount", 0,
                        "reportCount", 0,
                        "participants", List.of(
                                Map.of("deviceId", deviceId1, "tableName", tableName1, "isMuted", false),
                                Map.of("deviceId", deviceId2, "tableName", tableName2, "isMuted", false)
                        ),
                        "unreadCount", 0
                )
        ));

        analyticsLogService.logChatRoomCreated(chatRoom, deviceId1, deviceId2);

        log.info("Chat room created: roomId={}, {} ↔ {}", chatRoom.getId(), tableName1, tableName2);
        return chatRoom;
    }

    @Transactional
    public void closeRoom(Long roomId, String leavingDeviceId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(BusinessException::chatRoomNotFound);

        if (chatRoom.getStatus() != ChatRoomStatus.ACTIVE) {
            return;
        }

        List<ChatRoomParticipant> participants = chatRoomParticipantRepository.findByChatRoomId(roomId);

        // 삭제 전에 필요한 정보 추출 (벌크 삭제 후 엔티티 참조하면 flush 에러 발생)
        List<String> deviceIds = participants.stream()
                .map(ChatRoomParticipant::getDeviceId)
                .toList();
        String participantNames = participants.stream()
                .map(p -> p.getTableName() + "(" + p.getDeviceId() + ")")
                .collect(Collectors.joining(" ↔ "));

        // 상대방에게 알림
        for (String deviceId : deviceIds) {
            if (!deviceId.equals(leavingDeviceId)) {
                webSocketSenderService.sendChatToDevice(deviceId, Map.of(
                        "type", "CHAT_CLOSED",
                        "roomId", roomId,
                        "reason", "PARTICIPANT_LEFT"
                ));
            }
        }

        // 스태프 모니터에 알림
        webSocketSenderService.broadcast("staff.chat.monitor", Map.of(
                "type", "CHAT_ROOM_CLOSED",
                "roomId", roomId
        ));

        // 히스토리로 아카이브
        chatRoomHistoryRepository.save(ChatRoomHistory.builder()
                .roomId(roomId)
                .participants(participantNames)
                .status(ChatRoomStatus.CLOSED)
                .totalMessageCount(chatRoom.getTotalMessageCount())
                .giftCount(chatRoom.getGiftCount())
                .reportCount(chatRoom.getReportCount())
                .startedAt(chatRoom.getStartedAt())
                .closedAt(LocalDateTime.now())
                .deleteReason("PARTICIPANT_LEFT")
                .build());

        analyticsLogService.logChatRoomClosed(
                chatRoom,
                leavingDeviceId,
                deviceIds.stream().filter(id -> !id.equals(leavingDeviceId)).findFirst().orElse(null),
                "PARTICIPANT_LEFT"
        );

        // persistence context 정리 후 벌크 삭제 (managed 엔티티 참조 충돌 방지)
        entityManager.flush();
        entityManager.clear();

        purgeRoomData(roomId);

        // 참여자 테이블 상태 복원 (삭제 후 카운트해야 현재 방이 제외됨)
        for (String deviceId : deviceIds) {
            restoreTableChatStateIfIdle(deviceId);
        }

        log.info("Chat room closed and archived: roomId={}, leavingDeviceId={}", roomId, leavingDeviceId);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getActiveRooms() {
        return chatRoomRepository.findByStatusOrderByStartedAtDesc(ChatRoomStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getNotClosedRooms() {
        return chatRoomRepository.findAllNotClosedOrderByStartedAtDesc();
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
    public void sanctionRoom(Long roomId, Long userId, SanctionType type, String reason, Integer durationMinutes) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(BusinessException::chatRoomNotFound);

        if (chatRoom.getStatus() != ChatRoomStatus.ACTIVE) {
            return;
        }

        List<ChatRoomParticipant> participants = chatRoomParticipantRepository.findByChatRoomId(roomId);

        switch (type) {
            case WARNING -> applyWarning(chatRoom, roomId, userId, participants, reason);
            case MUTE -> applyMute(chatRoom, roomId, userId, participants, reason, durationMinutes);
            case BAN -> applyBan(chatRoom, roomId, userId, participants, reason, durationMinutes);
        }
    }

    private void applyWarning(ChatRoom chatRoom, Long roomId, Long userId,
                               List<ChatRoomParticipant> participants, String reason) {
        String systemMsg = reason != null && !reason.isBlank()
                ? "관리자 경고: " + reason
                : "관리자로부터 경고를 받았습니다.";
        chatMessageService.saveSystemMessage(chatRoom, systemMsg);

        for (ChatRoomParticipant p : participants) {
            webSocketSenderService.sendChatToDevice(p.getDeviceId(), Map.of(
                    "type", "CHAT_WARNING",
                    "roomId", roomId,
                    "reason", reason != null ? reason : ""
            ));
        }

        webSocketSenderService.broadcast("staff.chat.monitor", Map.of(
                "type", "CHAT_ROOM_UPDATED",
                "roomId", roomId,
                "reason", "WARNING"
        ));

        chatModerationHistoryService.recordRoomSanction(
                chatRoom, participants, userId, ChatModerationActionType.WARNING, reason, null
        );

        log.info("Chat room warned: roomId={}, by userId={}", roomId, userId);
    }

    private void applyMute(ChatRoom chatRoom, Long roomId, Long userId,
                            List<ChatRoomParticipant> participants, String reason, Integer durationMinutes) {
        LocalDateTime expiresAt = durationMinutes != null
                ? LocalDateTime.now().plusMinutes(durationMinutes) : null;

        chatRoom.sanction(SanctionType.MUTE, reason, expiresAt);

        String durationText = durationMinutes != null ? " (" + durationMinutes + "분)" : "";
        String systemMsg = reason != null && !reason.isBlank()
                ? "관리자에 의해 채팅이 음소거되었습니다" + durationText + ". 사유: " + reason
                : "관리자에 의해 채팅이 음소거되었습니다" + durationText + ".";
        chatMessageService.saveSystemMessage(chatRoom, systemMsg);

        for (ChatRoomParticipant p : participants) {
            p.setIsMuted(true);
            chatRoomParticipantRepository.save(p);

            webSocketSenderService.sendChatToDevice(p.getDeviceId(), Map.of(
                    "type", "CHAT_MUTED_BY_STAFF",
                    "roomId", roomId,
                    "reason", reason != null ? reason : "",
                    "expiresAt", expiresAt != null ? expiresAt.toString() : ""
            ));
        }

        if (durationMinutes != null) {
            redisTemplate.opsForValue().set(
                    "sanction:room:" + roomId,
                    SanctionType.MUTE.name(),
                    Duration.ofMinutes(durationMinutes)
            );
        }

        webSocketSenderService.broadcast("staff.chat.monitor", Map.of(
                "type", "CHAT_ROOM_UPDATED",
                "roomId", roomId,
                "reason", "MUTE"
        ));

        chatModerationHistoryService.recordRoomSanction(
                chatRoom, participants, userId, ChatModerationActionType.MUTE, reason, durationMinutes
        );

        log.info("Chat room muted: roomId={}, by userId={}, duration={}min", roomId, userId, durationMinutes);
    }

    private void applyBan(ChatRoom chatRoom, Long roomId, Long userId,
                           List<ChatRoomParticipant> participants, String reason, Integer durationMinutes) {
        LocalDateTime expiresAt = durationMinutes != null
                ? LocalDateTime.now().plusMinutes(durationMinutes) : null;

        chatRoom.sanction(SanctionType.BAN, reason, expiresAt);
        chatRoomRepository.save(chatRoom);

        String durationText = durationMinutes != null ? " (" + durationMinutes + "분)" : "";
        String systemMsg = reason != null && !reason.isBlank()
                ? "관리자에 의해 채팅이 제재되었습니다" + durationText + ". 사유: " + reason
                : "관리자에 의해 채팅이 제재되었습니다" + durationText + ".";
        chatMessageService.saveSystemMessage(chatRoom, systemMsg);

        for (ChatRoomParticipant p : participants) {
            webSocketSenderService.sendChatToDevice(p.getDeviceId(), Map.of(
                    "type", "CHAT_SANCTIONED",
                    "roomId", roomId,
                    "sanctionType", "BAN",
                    "reason", reason != null ? reason : "",
                    "expiresAt", expiresAt != null ? expiresAt.toString() : ""
            ));
        }

        // BAN 후 ACTIVE 방이 없으면 채팅중 상태 해제 (상태 변경 flush 이후 카운트)
        for (ChatRoomParticipant p : participants) {
            restoreTableChatStateIfIdle(p.getDeviceId());
        }

        if (durationMinutes != null) {
            redisTemplate.opsForValue().set(
                    "sanction:room:" + roomId,
                    SanctionType.BAN.name(),
                    Duration.ofMinutes(durationMinutes)
            );
        }

        webSocketSenderService.broadcast("staff.chat.monitor", Map.of(
                "type", "CHAT_ROOM_UPDATED",
                "roomId", roomId,
                "reason", "BAN"
        ));

        chatModerationHistoryService.recordRoomSanction(
                chatRoom, participants, userId, ChatModerationActionType.BAN, reason, durationMinutes
        );

        log.info("Chat room banned: roomId={}, by userId={}, duration={}min", roomId, userId, durationMinutes);
    }

    @Transactional
    public void liftSanction(Long roomId) {
        liftSanction(roomId, null);
    }

    @Transactional
    public void liftSanction(Long roomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(BusinessException::chatRoomNotFound);

        if (chatRoom.getStatus() != ChatRoomStatus.SANCTIONED) {
            return;
        }

        SanctionType previousType = chatRoom.getSanctionType();
        chatRoom.liftSanction();

        // MUTE 해제 시 참여자 뮤트 플래그도 해제
        if (previousType == SanctionType.MUTE) {
            List<ChatRoomParticipant> participants = chatRoomParticipantRepository.findByChatRoomId(roomId);
            for (ChatRoomParticipant p : participants) {
                p.setIsMuted(false);
                chatRoomParticipantRepository.save(p);
            }
        }

        String systemMsg = "제재가 해제되었습니다.";
        chatMessageService.saveSystemMessage(chatRoom, systemMsg);

        List<ChatRoomParticipant> participants = chatRoomParticipantRepository.findByChatRoomId(roomId);
        for (ChatRoomParticipant p : participants) {
            webSocketSenderService.sendChatToDevice(p.getDeviceId(), Map.of(
                    "type", "CHAT_SANCTION_LIFTED",
                    "roomId", roomId
            ));
        }

        // Redis TTL 키 제거
        redisTemplate.delete("sanction:room:" + roomId);

        webSocketSenderService.broadcast("staff.chat.monitor", Map.of(
                "type", "ROOM_SANCTION_LIFTED",
                "roomId", roomId
        ));

        chatModerationHistoryService.recordSanctionLift(chatRoom, participants, userId, previousType);

        log.info("Sanction lifted: roomId={}, previousType={}", roomId, previousType);
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

        // 삭제 전에 필요한 정보 추출 (벌크 삭제 후 엔티티 참조하면 flush 에러 발생)
        List<String> deviceIds = participants.stream()
                .map(ChatRoomParticipant::getDeviceId)
                .toList();
        String participantNames = participants.stream()
                .map(p -> p.getTableName() + "(" + p.getDeviceId() + ")")
                .collect(Collectors.joining(" ↔ "));
        boolean wasActive = room.getStatus() == ChatRoomStatus.ACTIVE;

        // 히스토리 저장
        chatRoomHistoryRepository.save(ChatRoomHistory.builder()
                .roomId(roomId)
                .participants(participantNames)
                .status(room.getStatus())
                .totalMessageCount(room.getTotalMessageCount())
                .giftCount(room.getGiftCount())
                .reportCount(room.getReportCount())
                .startedAt(room.getStartedAt())
                .closedAt(room.getClosedAt())
                .sanctionType(room.getSanctionType())
                .sanctionReason(room.getSanctionReason())
                .deleteReason(deleteReason)
                .build());

        // persistence context 정리 후 벌크 삭제 (managed 엔티티 참조 충돌 방지)
        entityManager.flush();
        entityManager.clear();

        purgeRoomData(roomId);

        // 참여자 테이블 상태 복원 (삭제 트리거 디바이스 제외)
        if (wasActive || room.getStatus() == ChatRoomStatus.SANCTIONED) {
            for (String deviceId : deviceIds) {
                if (!deviceId.equals(triggerDeviceId)) {
                    webSocketSenderService.sendChatToDevice(deviceId, Map.of(
                            "type", "CHAT_CLOSED",
                            "roomId", roomId,
                            "reason", "PARTICIPANT_DELETED"
                    ));
                }
                restoreTableChatStateIfIdle(deviceId);
            }
        }

        log.info("Chat room deleted and archived: roomId={}, reason={}", roomId, deleteReason);
    }

    /**
     * 디바이스가 참여 중인 전체 채팅방 스냅샷 조회 (sync용)
     * ACTIVE / CLOSED / SANCTIONED 모두 포함
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChatRoomsSnapshot(String deviceId) {
        List<ChatRoomParticipant> myParticipations = chatRoomParticipantRepository.findAllWithChatRoomByDeviceId(deviceId);

        return myParticipations.stream().map(myP -> {
            ChatRoom chatRoom = myP.getChatRoom();
            Long roomId = chatRoom.getId();

            // 상대방 찾기
            ChatRoomParticipant partner = chatRoomParticipantRepository.findByChatRoomId(roomId).stream()
                    .filter(p -> !p.getDeviceId().equals(deviceId))
                    .findFirst()
                    .orElse(null);

            // 최근 50건 메시지 (DESC로 가져와서 ASC로 정렬)
            List<ChatMessage> recentMessages = chatMessageRepository
                    .findTop50ByChatRoomIdOrderByCreatedAtDesc(roomId);
            java.util.Collections.reverse(recentMessages);

            List<Map<String, Object>> messages = recentMessages.stream()
                    .map(msg -> {
                        Map<String, Object> m = new java.util.LinkedHashMap<>();
                        m.put("id", msg.getId());
                        m.put("chatRoomId", roomId);
                        m.put("senderDeviceId", msg.getSenderDeviceId());
                        m.put("senderTableName", msg.getSenderTableName());
                        m.put("content", msg.getContent());
                        m.put("type", msg.getType());
                        m.put("createdAt", msg.getCreatedAt());
                        return m;
                    })
                    .toList();

            Map<String, Object> room = new java.util.LinkedHashMap<>();
            room.put("roomId", roomId);
            room.put("status", chatRoom.getStatus());
            room.put("partnerDeviceId", partner != null ? partner.getDeviceId() : null);
            room.put("partnerTableName", partner != null ? partner.getTableName() : null);
            room.put("messages", messages);
            if (chatRoom.getStatus() == ChatRoomStatus.SANCTIONED) {
                room.put("sanctionType", chatRoom.getSanctionType());
                room.put("sanctionReason", chatRoom.getSanctionReason());
                room.put("sanctionExpiresAt", chatRoom.getSanctionExpiresAt() != null
                        ? chatRoom.getSanctionExpiresAt().toString() : null);
            }
            return room;
        }).toList();
    }

    /**
     * 디바이스 목록에 대한 채팅 상태 배치 조회
     */
    @Transactional(readOnly = true)
    public Map<String, ChatStatusInfo> getChatStatusByDeviceIds(List<String> deviceIds) {
        if (deviceIds.isEmpty()) return Map.of();

        List<ChatRoomParticipant> participants =
                chatRoomParticipantRepository.findActiveOrSanctionedByDeviceIds(deviceIds);

        Map<String, ChatStatusInfo> result = new java.util.HashMap<>();
        for (ChatRoomParticipant p : participants) {
            ChatRoom room = p.getChatRoom();
            SanctionType type = (room.getStatus() == ChatRoomStatus.SANCTIONED)
                    ? room.getSanctionType() : null;
            LocalDateTime expiresAt = (room.getStatus() == ChatRoomStatus.SANCTIONED)
                    ? room.getSanctionExpiresAt() : null;
            // MUTE는 SANCTIONED가 아니어도 개별 참여자에게 적용될 수 있음
            if (room.getSanctionType() == SanctionType.MUTE) {
                type = SanctionType.MUTE;
                expiresAt = room.getSanctionExpiresAt();
            }
            result.put(p.getDeviceId(), new ChatStatusInfo(room.getId(), type, p.getIsMuted(), expiresAt));
        }
        return result;
    }

    public record ChatStatusInfo(Long chatRoomId, SanctionType sanctionType, Boolean isMuted, LocalDateTime sanctionExpiresAt) {}

    private void broadcastTableUpdated(TableEntity table) {
        // 채팅 상태 포함하여 브로드캐스트
        Map<String, ChatStatusInfo> statusMap = getChatStatusByDeviceIds(List.of(table.getId()));
        ChatStatusInfo info = statusMap.get(table.getId());

        TableListResponse data = info != null
                ? TableListResponse.from(table, info.chatRoomId(), info.sanctionType(), info.isMuted(), info.sanctionExpiresAt())
                : TableListResponse.from(table);

        webSocketSenderService.broadcast("tables", Map.of(
                "type", "TABLE_UPDATED",
                "data", data,
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    private void broadcastTableWithChatInfo(TableEntity table, Long chatRoomId,
                                             SanctionType sanctionType, Boolean isMuted,
                                             LocalDateTime sanctionExpiresAt) {
        TableListResponse data = TableListResponse.from(table, chatRoomId, sanctionType, isMuted, sanctionExpiresAt);
        webSocketSenderService.broadcast("tables", Map.of(
                "type", "TABLE_UPDATED",
                "data", data,
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    private void purgeRoomData(Long roomId) {
        chatMessageRepository.deleteByChatRoomId(roomId);
        chatReportRepository.deleteByChatRoomId(roomId);
        staffChatReadPositionRepository.deleteByChatRoomId(roomId);
        chatRoomParticipantRepository.deleteByChatRoomId(roomId);
        entityManager.flush();
        chatRoomRepository.deleteByRoomId(roomId);
        entityManager.flush();
    }

    private void restoreTableChatStateIfIdle(String deviceId) {
        if (chatRoomParticipantRepository.countActiveOrSanctionedRoomsByDeviceId(deviceId) == 0) {
            tableRepository.findById(deviceId).ifPresent(table -> {
                table.endChatting();
                TableEntity saved = tableRepository.save(table);
                broadcastTableUpdated(saved);
            });
        }
    }
}
