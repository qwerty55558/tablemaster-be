package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.entity.*;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.repository.ChatReportRepository;
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
    private final WebSocketSenderService webSocketSenderService;

    @Transactional
    public ChatReport createReport(Long chatRoomId, String reporterDeviceId,
                                    String reportedDeviceId, String reason) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(BusinessException::chatRoomNotFound);

        ChatReport report = ChatReport.builder()
                .chatRoom(chatRoom)
                .reporterDeviceId(reporterDeviceId)
                .reportedDeviceId(reportedDeviceId)
                .reason(reason)
                .build();

        ChatReport saved = chatReportRepository.save(report);
        chatRoom.incrementReportCount();
        chatRoomRepository.save(chatRoom);

        // 스태프에게 신고 알림
        webSocketSenderService.broadcast("staff.chat.monitor", Map.of(
                "type", "REPORT_CREATED",
                "roomId", chatRoomId,
                "reportCount", chatRoom.getReportCount()
        ));

        log.info("Chat report created: roomId={}, reporter={}, reported={}", chatRoomId, reporterDeviceId, reportedDeviceId);
        return saved;
    }

    @Transactional
    public ChatReport reviewReport(Long reportId, Long userId, ChatReportStatus status) {
        ChatReport report = chatReportRepository.findById(reportId)
                .orElseThrow(BusinessException::chatReportNotFound);

        report.review(userId, status);
        chatReportRepository.save(report);

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
