package com.mycompany.tablemaster.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatReportRequest {
    @NotBlank
    private String reportedDeviceId;

    @NotBlank
    private String reason;
}
