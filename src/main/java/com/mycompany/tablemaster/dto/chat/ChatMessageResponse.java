package com.mycompany.tablemaster.dto.chat;

import com.mycompany.tablemaster.entity.ChatMessage;
import com.mycompany.tablemaster.entity.ChatMessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long chatRoomId;
    private String senderDeviceId;
    private String senderTableName;
    private String content;
    private ChatMessageType type;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderDeviceId(message.getSenderDeviceId())
                .senderTableName(message.getSenderTableName())
                .content(message.getContent())
                .type(message.getType())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
