package com.mycompany.tablemaster.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatNotifyRequest {
    private String title;
    private String message;
}
