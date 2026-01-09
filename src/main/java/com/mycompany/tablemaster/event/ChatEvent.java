package com.mycompany.tablemaster.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ChatEvent(
    Long roomId,
    Long senderId,
    String message,
    ChatEventType type,
    LocalDateTime timestamp
) implements Serializable {

    public static ChatEvent message(Long roomId, Long senderId, String message) {
        return new ChatEvent(roomId, senderId, message, ChatEventType.MESSAGE, LocalDateTime.now());
    }

    public static ChatEvent join(Long roomId, Long senderId) {
        return new ChatEvent(roomId, senderId, null, ChatEventType.JOIN, LocalDateTime.now());
    }

    public static ChatEvent leave(Long roomId, Long senderId) {
        return new ChatEvent(roomId, senderId, null, ChatEventType.LEAVE, LocalDateTime.now());
    }
}
