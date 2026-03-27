package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.entity.*;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.repository.ChatReportRepository;
import com.mycompany.tablemaster.repository.ChatRoomParticipantRepository;
import com.mycompany.tablemaster.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatReportService {

    private final ChatReportRepository chatReportRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatModerationHistoryService chatModerationHistoryService;
    private final WebSocketSenderService webSocketSenderService;
    private final AnalyticsLogService analyticsLogService;

    @Transactional
    public ChatReport createReport(Long chatRoomId, String reporterDeviceId,
                                    String reportedDeviceId, String reason) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(BusinessException::chatRoomNotFound);

        if (reporterDeviceId.equals(reportedDeviceId)) {
            throw BusinessException.chatReportSelfNotAllowed();
        }

        ChatRoomParticipant reporterParticipant = chatRoomParticipantRepository
                .findByChatRoomIdAndDeviceId(chatRoomId, reporterDeviceId)
                .orElseThrow(BusinessException::chatReportParticipantMismatch);
        ChatRoomParticipant reportedParticipant = chatRoomParticipantRepository
                .findByChatRoomIdAndDeviceId(chatRoomId, reportedDeviceId)
                .orElseThrow(BusinessException::chatReportParticipantMismatch);

        if (chatReportRepository.existsByChatRoomIdAndReporterDeviceIdAndReportedDeviceIdAndStatus(
                chatRoomId, reporterDeviceId, reportedDeviceId, ChatReportStatus.PENDING
        )) {
            throw BusinessException.chatReportAlreadyPending();
        }

        ChatReport report = ChatReport.builder()
                .chatRoom(chatRoom)
                .reporterDeviceId(reporterDeviceId)
                .reporterTableName(reporterParticipant.getTableName())
                .reportedDeviceId(reportedDeviceId)
                .reportedTableName(reportedParticipant.getTableName())
                .reason(reason)
                .build();

        ChatReport saved = chatReportRepository.save(report);
        chatRoomRepository.incrementReportCount(chatRoom.getId());
        ChatRoom refreshedRoom = chatRoomRepository.findById(chatRoomId).orElse(chatRoom);

        Map<String, Object> reportPayload = Map.ofEntries(
                Map.entry("type", "REPORT_CREATED"),
                Map.entry("reportId", saved.getId()),
                Map.entry("roomId", chatRoomId),
                Map.entry("reporterDeviceId", reporterDeviceId),
                Map.entry("reporterTableName", saved.getReporterTableName()),
                Map.entry("reportedDeviceId", reportedDeviceId),
                Map.entry("reportedTableName", saved.getReportedTableName()),
                Map.entry("reason", reason),
                Map.entry("status", saved.getStatus().name()),
                Map.entry("createdAt", saved.getCreatedAt().toString()),
                Map.entry("reportCount", refreshedRoom.getReportCount())
        );

        // 관리자 모니터 실시간 갱신
        webSocketSenderService.broadcast("staff.chat.monitor", reportPayload);

        analyticsLogService.logChatReportCreated(chatRoom, saved);

        log.info("Chat report created: roomId={}, reporter={}({}), reported={}({})",
                chatRoomId,
                reporterDeviceId,
                saved.getReporterTableName(),
                reportedDeviceId,
                saved.getReportedTableName());
        return saved;
    }

    @Transactional
    public ChatReport reviewReport(Long reportId, Long userId, ChatReportStatus status) {
        ChatReport report = chatReportRepository.findById(reportId)
                .orElseThrow(BusinessException::chatReportNotFound);

        report.review(userId, status);
        chatReportRepository.save(report);
        String tableNames = chatRoomParticipantRepository.findByChatRoomId(report.getChatRoom().getId()).stream()
                .map(ChatRoomParticipant::getTableName)
                .distinct()
                .reduce((left, right) -> left + " ↔ " + right)
                .orElse("UNKNOWN");
        chatModerationHistoryService.recordReportReview(report, tableNames, userId, status);

        log.info("Chat report reviewed: reportId={}, status={}, reviewedBy={}", reportId, status, userId);
        return report;
    }

    @Transactional(readOnly = true)
    public List<ChatReport> getReportsByRoom(Long chatRoomId) {
        return chatReportRepository.findByChatRoomId(chatRoomId);
    }

    @Transactional(readOnly = true)
    public List<ChatReport> getPendingReports() {
        return chatReportRepository.findByStatus(ChatReportStatus.PENDING);
    }
}
