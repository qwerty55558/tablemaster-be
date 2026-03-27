package com.mycompany.tablemaster.dto.chat;

import com.mycompany.tablemaster.entity.ChatModerationHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatModerationHistoryDetailResponse {
    private Long id;
    private Long chatRoomId;
    private String tableNames;
    private String reason;
    private String action;
    private String actionDetail;
    private Long reportId;
    private Long processedByUserId;
    private String processedBy;
    private LocalDateTime processedAt;
    private String payload;

    public static ChatModerationHistoryDetailResponse from(ChatModerationHistory history) {
        return ChatModerationHistoryDetailResponse.builder()
                .id(history.getId())
                .chatRoomId(history.getChatRoomId())
                .tableNames(history.getTableNames())
                .reason(history.getReason())
                .action(history.getActionType().name())
                .actionDetail(history.getActionDetail())
                .reportId(history.getReportId())
                .processedByUserId(history.getProcessedByUserId())
                .processedBy(history.getProcessedByName())
                .processedAt(history.getProcessedAt())
                .payload(history.getPayload())
                .build();
    }
}
