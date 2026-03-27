package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.dto.analytics.AnalyticsSummaryResponse;
import com.mycompany.tablemaster.entity.ChatAnalyticsEventType;
import com.mycompany.tablemaster.entity.ChatAnalyticsLog;
import com.mycompany.tablemaster.entity.VisitorAnalyticsEventType;
import com.mycompany.tablemaster.entity.VisitorAnalyticsLog;
import com.mycompany.tablemaster.repository.ChatAnalyticsLogRepository;
import com.mycompany.tablemaster.repository.VisitorAnalyticsLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsQueryService {

    private final VisitorAnalyticsLogRepository visitorAnalyticsLogRepository;
    private final ChatAnalyticsLogRepository chatAnalyticsLogRepository;

    public AnalyticsSummaryResponse getSummary(LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(6);
        LocalDate endDate = to != null ? to : LocalDate.now();

        LocalDateTime fromDateTime = startDate.atStartOfDay();
        LocalDateTime toDateTime = endDate.plusDays(1).atStartOfDay().minusNanos(1);

        List<VisitorAnalyticsLog> visitorLogs =
                visitorAnalyticsLogRepository.findByLoggedAtBetweenOrderByLoggedAtAsc(fromDateTime, toDateTime);
        List<ChatAnalyticsLog> chatLogs =
                chatAnalyticsLogRepository.findByLoggedAtBetweenOrderByLoggedAtAsc(fromDateTime, toDateTime);

        Map<LocalDate, DailyAccumulator> dailyMap = new LinkedHashMap<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            dailyMap.put(cursor, new DailyAccumulator());
            cursor = cursor.plusDays(1);
        }

        long enterCount = 0;
        long exitCount = 0;
        long reconnectCount = 0;
        long updateCount = 0;
        long totalGuestCount = 0;

        for (VisitorAnalyticsLog log : visitorLogs) {
            DailyAccumulator daily = dailyMap.computeIfAbsent(log.getLoggedAt().toLocalDate(), ignored -> new DailyAccumulator());
            switch (log.getEventType()) {
                case ENTER -> {
                    enterCount++;
                    daily.visitorEnterCount++;
                    totalGuestCount += log.getGuestCount() != null ? log.getGuestCount() : 0;
                }
                case EXIT -> {
                    exitCount++;
                    daily.visitorExitCount++;
                }
                case RECONNECT -> reconnectCount++;
                case UPDATE -> updateCount++;
            }
        }

        long roomCreatedCount = 0;
        long roomClosedCount = 0;
        long messageCount = 0;
        long giftCount = 0;
        long reportCount = 0;

        for (ChatAnalyticsLog log : chatLogs) {
            DailyAccumulator daily = dailyMap.computeIfAbsent(log.getLoggedAt().toLocalDate(), ignored -> new DailyAccumulator());
            switch (log.getEventType()) {
                case ROOM_CREATED, ROOM_REMAPPED -> roomCreatedCount++;
                case ROOM_CLOSED -> roomClosedCount++;
                case MESSAGE_SENT -> {
                    messageCount++;
                    daily.chatMessageCount++;
                }
                case GIFT_SENT -> {
                    giftCount++;
                    daily.chatGiftCount++;
                }
                case REPORT_CREATED -> {
                    reportCount++;
                    daily.chatReportCount++;
                }
            }
        }

        return AnalyticsSummaryResponse.builder()
                .from(fromDateTime)
                .to(toDateTime)
                .visitor(AnalyticsSummaryResponse.VisitorSummary.builder()
                        .enterCount(enterCount)
                        .exitCount(exitCount)
                        .reconnectCount(reconnectCount)
                        .updateCount(updateCount)
                        .totalGuestCount(totalGuestCount)
                        .build())
                .chat(AnalyticsSummaryResponse.ChatSummary.builder()
                        .roomCreatedCount(roomCreatedCount)
                        .roomClosedCount(roomClosedCount)
                        .messageCount(messageCount)
                        .giftCount(giftCount)
                        .reportCount(reportCount)
                        .build())
                .daily(dailyMap.entrySet().stream()
                        .map(entry -> AnalyticsSummaryResponse.DailyPoint.builder()
                                .date(entry.getKey())
                                .visitorEnterCount(entry.getValue().visitorEnterCount)
                                .visitorExitCount(entry.getValue().visitorExitCount)
                                .chatMessageCount(entry.getValue().chatMessageCount)
                                .chatGiftCount(entry.getValue().chatGiftCount)
                                .chatReportCount(entry.getValue().chatReportCount)
                                .build())
                        .toList())
                .build();
    }

    private static class DailyAccumulator {
        long visitorEnterCount;
        long visitorExitCount;
        long chatMessageCount;
        long chatGiftCount;
        long chatReportCount;
    }
}
