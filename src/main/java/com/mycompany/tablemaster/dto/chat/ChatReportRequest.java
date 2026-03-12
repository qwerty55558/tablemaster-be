package com.mycompany.tablemaster.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatReportRequest {
    private String reportedDeviceId;
    private String reason;
}
