package com.mycompany.tablemaster.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ChatEvent(
    Long roomId,
    Long senderId,
    String senderDeviceId,
    String senderTableName,
    String message,
    ChatEventType type,
    String messageType,
    LocalDateTime timestamp
) implements Serializable {

    public static ChatEvent message(Long roomId, String senderDeviceId, String senderTableName, String message) {
        return new ChatEvent(roomId, null, senderDeviceId, senderTableName, message, ChatEventType.MESSAGE, "TEXT", LocalDateTime.now());
    }

    public static ChatEvent gift(Long roomId, String senderDeviceId, String senderTableName, String giftType) {
        return new ChatEvent(roomId, null, senderDeviceId, senderTableName, giftType, ChatEventType.MESSAGE, "GIFT", LocalDateTime.now());
    }

    public static ChatEvent join(Long roomId, String senderDeviceId, String senderTableName) {
        return new ChatEvent(roomId, null, senderDeviceId, senderTableName, null, ChatEventType.JOIN, null, LocalDateTime.now());
    }

    public static ChatEvent leave(Long roomId, String senderDeviceId, String senderTableName) {
        return new ChatEvent(roomId, null, senderDeviceId, senderTableName, null, ChatEventType.LEAVE, null, LocalDateTime.now());
    }

    public static ChatEvent system(Long roomId, String message) {
        return new ChatEvent(roomId, null, null, null, message, ChatEventType.MESSAGE, "SYSTEM", LocalDateTime.now());
    }

    // Legacy factory methods for backward compatibility
    @Deprecated
    public static ChatEvent message(Long roomId, Long senderId, String message) {
        return new ChatEvent(roomId, senderId, null, null, message, ChatEventType.MESSAGE, "TEXT", LocalDateTime.now());
    }

    @Deprecated
    public static ChatEvent join(Long roomId, Long senderId) {
        return new ChatEvent(roomId, senderId, null, null, null, ChatEventType.JOIN, null, LocalDateTime.now());
    }

    @Deprecated
    public static ChatEvent leave(Long roomId, Long senderId) {
        return new ChatEvent(roomId, senderId, null, null, null, ChatEventType.LEAVE, null, LocalDateTime.now());
    }
}
