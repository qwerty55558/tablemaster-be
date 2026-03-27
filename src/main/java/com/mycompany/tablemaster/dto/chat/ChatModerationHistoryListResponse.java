package com.mycompany.tablemaster.dto.chat;

import com.mycompany.tablemaster.entity.ChatModerationHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatModerationHistoryListResponse {
    private Long id;
    private Long chatRoomId;
    private String tableNames;
    private String reason;
    private String action;
    private LocalDateTime processedAt;
    private String processedBy;

    public static ChatModerationHistoryListResponse from(ChatModerationHistory history) {
        return ChatModerationHistoryListResponse.builder()
                .id(history.getId())
                .chatRoomId(history.getChatRoomId())
                .tableNames(history.getTableNames())
                .reason(history.getReason())
                .action(history.getActionType().name())
                .processedAt(history.getProcessedAt())
                .processedBy(history.getProcessedByName())
                .build();
    }
}
