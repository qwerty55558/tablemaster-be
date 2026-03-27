package com.mycompany.tablemaster.dto.chat;

import com.mycompany.tablemaster.entity.ChatReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatReportReviewRequest {
    @NotNull
    private ChatReportStatus status;
}
