package com.mycompany.tablemaster.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AnalyticsSummaryResponse {
    private LocalDateTime from;
    private LocalDateTime to;
    private VisitorSummary visitor;
    private ChatSummary chat;
    private List<DailyPoint> daily;

    @Getter
    @Builder
    public static class VisitorSummary {
        private long enterCount;
        private long exitCount;
        private long reconnectCount;
        private long updateCount;
        private long totalGuestCount;
    }

    @Getter
    @Builder
    public static class ChatSummary {
        private long roomCreatedCount;
        private long roomClosedCount;
        private long messageCount;
        private long giftCount;
        private long reportCount;
    }

    @Getter
    @Builder
    public static class DailyPoint {
        private LocalDate date;
        private long visitorEnterCount;
        private long visitorExitCount;
        private long chatMessageCount;
        private long chatGiftCount;
        private long chatReportCount;
    }
}
