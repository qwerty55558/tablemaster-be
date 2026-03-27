package com.mycompany.tablemaster.dto.chat;

import com.mycompany.tablemaster.entity.ChatReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatReportResponse {
    private Long id;
    private Long chatRoomId;
    private String reporterTableName;
    private String reportedTableName;
    private String reason;
    private String status;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    public static ChatReportResponse from(ChatReport report) {
        return ChatReportResponse.builder()
                .id(report.getId())
                .chatRoomId(report.getChatRoom().getId())
                .reporterTableName(report.getReporterTableName())
                .reportedTableName(report.getReportedTableName())
                .reason(report.getReason())
                .status(report.getStatus().name())
                .reviewedBy(report.getReviewedBy())
                .reviewedAt(report.getReviewedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
