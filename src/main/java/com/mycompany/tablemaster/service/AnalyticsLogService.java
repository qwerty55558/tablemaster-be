package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.entity.*;
import com.mycompany.tablemaster.repository.ChatAnalyticsLogRepository;
import com.mycompany.tablemaster.repository.VisitorAnalyticsLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsLogService {

    private final VisitorAnalyticsLogRepository visitorAnalyticsLogRepository;
    private final ChatAnalyticsLogRepository chatAnalyticsLogRepository;

    @Transactional
    public void logVisitorEntry(TableEntity table) {
        saveVisitorLog(table, VisitorAnalyticsEventType.ENTER, null);
    }

    @Transactional
    public void logVisitorUpdate(TableEntity table) {
        saveVisitorLog(table, VisitorAnalyticsEventType.UPDATE, null);
    }

    @Transactional
    public void logVisitorExit(TableEntity table, String reason) {
        saveVisitorLog(table, VisitorAnalyticsEventType.EXIT, reason);
    }

    @Transactional
    public void logVisitorReconnect(TableEntity table) {
        saveVisitorLog(table, VisitorAnalyticsEventType.RECONNECT, null);
    }

    @Transactional
    public void logChatRoomCreated(ChatRoom chatRoom, String actorDeviceId, String partnerDeviceId) {
        saveChatLog(chatRoom, ChatAnalyticsEventType.ROOM_CREATED, actorDeviceId, partnerDeviceId, null, null, null);
    }

    @Transactional
    public void logChatRoomRemapped(ChatRoom chatRoom, String actorDeviceId, String partnerDeviceId) {
        saveChatLog(chatRoom, ChatAnalyticsEventType.ROOM_REMAPPED, actorDeviceId, partnerDeviceId, null, null, "RECONNECTED");
    }

    @Transactional
    public void logChatRoomClosed(ChatRoom chatRoom, String actorDeviceId, String partnerDeviceId, String reason) {
        saveChatLog(chatRoom, ChatAnalyticsEventType.ROOM_CLOSED, actorDeviceId, partnerDeviceId, null, null, reason);
    }

    @Transactional
    public void logChatMessage(ChatRoom chatRoom, ChatMessage message) {
        ChatAnalyticsEventType eventType = message.getType() == ChatMessageType.GIFT
                ? ChatAnalyticsEventType.GIFT_SENT
                : ChatAnalyticsEventType.MESSAGE_SENT;
        saveChatLog(chatRoom, eventType, message.getSenderDeviceId(), null, message.getId(), null, message.getType().name());
    }

    @Transactional
    public void logChatReportCreated(ChatRoom chatRoom, ChatReport report) {
        saveChatLog(chatRoom, ChatAnalyticsEventType.REPORT_CREATED,
                report.getReporterDeviceId(), report.getReportedDeviceId(), null, report.getId(), report.getReason());
    }

    private void saveVisitorLog(TableEntity table, VisitorAnalyticsEventType eventType, String reason) {
        visitorAnalyticsLogRepository.save(VisitorAnalyticsLog.builder()
                .eventType(eventType)
                .deviceId(table.getId())
                .tableName(table.getName())
                .deviceName(table.getDeviceName())
                .location(table.getLocation())
                .guestCount(table.getGuestCount())
                .femaleCount(table.getFemaleCount())
                .maleCount(table.getMaleCount())
                .revenue(table.getRevenue())
                .tableStatus(table.getStatus())
                .reason(reason)
                .build());
        log.debug("Visitor analytics logged: deviceId={}, event={}", table.getId(), eventType);
    }

    private void saveChatLog(ChatRoom chatRoom, ChatAnalyticsEventType eventType, String actorDeviceId,
                             String partnerDeviceId, Long messageId, Long reportId, String reason) {
        chatAnalyticsLogRepository.save(ChatAnalyticsLog.builder()
                .eventType(eventType)
                .chatRoomId(chatRoom.getId())
                .actorDeviceId(actorDeviceId)
                .partnerDeviceId(partnerDeviceId)
                .messageId(messageId)
                .reportId(reportId)
                .messageType(reason != null && (eventType == ChatAnalyticsEventType.MESSAGE_SENT || eventType == ChatAnalyticsEventType.GIFT_SENT)
                        ? reason : null)
                .totalMessageCount(chatRoom.getTotalMessageCount())
                .giftCount(chatRoom.getGiftCount())
                .reportCount(chatRoom.getReportCount())
                .reason(eventType == ChatAnalyticsEventType.MESSAGE_SENT || eventType == ChatAnalyticsEventType.GIFT_SENT ? null : reason)
                .build());
        log.debug("Chat analytics logged: roomId={}, event={}", chatRoom.getId(), eventType);
    }
}
