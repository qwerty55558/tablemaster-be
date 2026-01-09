package com.mycompany.tablemaster.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record GiftEvent(
    Long giftId,
    Long senderId,
    Long receiverId,
    String productName,
    GiftEventType type,
    LocalDateTime timestamp
) implements Serializable {

    public static GiftEvent created(Long giftId, Long senderId, Long receiverId, String productName) {
        return new GiftEvent(giftId, senderId, receiverId, productName, GiftEventType.CREATED, LocalDateTime.now());
    }

    public static GiftEvent processing(Long giftId, Long senderId, Long receiverId, String productName) {
        return new GiftEvent(giftId, senderId, receiverId, productName, GiftEventType.PROCESSING, LocalDateTime.now());
    }

    public static GiftEvent completed(Long giftId, Long senderId, Long receiverId, String productName) {
        return new GiftEvent(giftId, senderId, receiverId, productName, GiftEventType.COMPLETED, LocalDateTime.now());
    }

    public static GiftEvent failed(Long giftId, Long senderId, Long receiverId, String productName) {
        return new GiftEvent(giftId, senderId, receiverId, productName, GiftEventType.FAILED, LocalDateTime.now());
    }
}
