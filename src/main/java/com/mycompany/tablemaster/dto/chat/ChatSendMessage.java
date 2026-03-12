package com.mycompany.tablemaster.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatSendMessage {
    private Long roomId;
    private String content;
}
