package com.mycompany.tablemaster.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.tablemaster.dto.chat.ChatModerationHistoryDetailResponse;
import com.mycompany.tablemaster.dto.chat.ChatModerationHistoryListResponse;
import com.mycompany.tablemaster.entity.*;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.repository.ChatModerationHistoryRepository;
import com.mycompany.tablemaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatModerationHistoryService {

    private final ChatModerationHistoryRepository chatModerationHistoryRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordRoomSanction(ChatRoom chatRoom, List<ChatRoomParticipant> participants, Long userId,
                                   ChatModerationActionType actionType, String reason, Integer durationMinutes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("durationMinutes", durationMinutes);
        payload.put("participants", participants.stream()
                .map(p -> Map.of(
                        "deviceId", p.getDeviceId(),
                        "tableName", p.getTableName(),
                        "isMuted", p.getIsMuted()
                ))
                .toList());

        saveHistory(chatRoom.getId(), summarizeTables(participants), actionType, reason,
                buildSanctionDetail(actionType, durationMinutes, participants),
                null, userId, buildPayload(payload));
    }

    @Transactional
    public void recordSanctionLift(ChatRoom chatRoom, List<ChatRoomParticipant> participants, Long userId,
                                   SanctionType previousType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("previousType", previousType != null ? previousType.name() : null);

        saveHistory(chatRoom.getId(), summarizeTables(participants), ChatModerationActionType.SANCTION_LIFTED,
                previousType != null ? previousType.name() + " 제재 해제" : "제재 해제",
                "기존 제재 유형: " + (previousType != null ? previousType.name() : "UNKNOWN"),
                null, userId, buildPayload(payload));
    }

    @Transactional
    public void recordReportReview(ChatReport report, String tableNames, Long userId, ChatReportStatus status) {
        ChatModerationActionType actionType = status == ChatReportStatus.REVIEWED
                ? ChatModerationActionType.REPORT_APPROVED
                : ChatModerationActionType.REPORT_REJECTED;

        saveHistory(report.getChatRoom().getId(), tableNames, actionType, report.getReason(),
                "신고 처리 상태: " + status.name(), report.getId(), userId, buildPayload(Map.of(
                        "reporterDeviceId", report.getReporterDeviceId(),
                        "reporterTableName", report.getReporterTableName(),
                        "reportedDeviceId", report.getReportedDeviceId(),
                        "reportedTableName", report.getReportedTableName(),
                        "status", status.name()
                )));
    }

    @Transactional(readOnly = true)
    public List<ChatModerationHistoryListResponse> getModerationHistories() {
        return chatModerationHistoryRepository.findAllByOrderByProcessedAtDesc().stream()
                .map(ChatModerationHistoryListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatModerationHistoryDetailResponse getModerationHistory(Long id) {
        ChatModerationHistory history = chatModerationHistoryRepository.findById(id)
                .orElseThrow(BusinessException::chatModerationHistoryNotFound);
        return ChatModerationHistoryDetailResponse.from(history);
    }

    private void saveHistory(Long chatRoomId, String tableNames, ChatModerationActionType actionType,
                             String reason, String actionDetail, Long reportId, Long userId, String payload) {
        String processorName = resolveUserName(userId);

        ChatModerationHistory history = ChatModerationHistory.builder()
                .chatRoomId(chatRoomId)
                .tableNames(tableNames)
                .actionType(actionType)
                .reason(reason)
                .actionDetail(actionDetail)
                .reportId(reportId)
                .processedByUserId(userId)
                .processedByName(processorName)
                .payload(payload)
                .build();

        chatModerationHistoryRepository.save(history);
        log.info("Chat moderation history saved: roomId={}, action={}, by={}", chatRoomId, actionType, userId);
    }

    private String summarizeTables(List<ChatRoomParticipant> participants) {
        return participants.stream()
                .map(ChatRoomParticipant::getTableName)
                .distinct()
                .reduce((left, right) -> left + " ↔ " + right)
                .orElse("UNKNOWN");
    }

    private String buildSanctionDetail(ChatModerationActionType actionType, Integer durationMinutes,
                                       List<ChatRoomParticipant> participants) {
        String durationText = durationMinutes != null ? durationMinutes + "분" : "제한 없음";
        String targets = participants.stream()
                .map(p -> p.getTableName() + "(" + p.getDeviceId() + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return "조치=" + actionType.name() + ", duration=" + durationText + ", 대상=" + targets;
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return "SYSTEM";
        }
        return userRepository.findById(userId)
                .map(User::getName)
                .orElse("UNKNOWN");
    }

    private String buildPayload(Map<String, Object> payload) {
        try {
            Map<String, Object> orderedPayload = new LinkedHashMap<>(payload);
            return objectMapper.writeValueAsString(orderedPayload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize moderation payload", e);
            return "{}";
        }
    }
}
