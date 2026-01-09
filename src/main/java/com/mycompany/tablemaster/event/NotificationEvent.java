package com.mycompany.tablemaster.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

public record NotificationEvent(
    Long userId,
    String title,
    String body,
    NotificationType type,
    Map<String, Object> data,
    LocalDateTime timestamp
) implements Serializable {

    public static NotificationEvent push(Long userId, String title, String body, Map<String, Object> data) {
        return new NotificationEvent(userId, title, body, NotificationType.PUSH, data, LocalDateTime.now());
    }

    public static NotificationEvent inApp(Long userId, String title, String body, Map<String, Object> data) {
        return new NotificationEvent(userId, title, body, NotificationType.IN_APP, data, LocalDateTime.now());
    }

    public static NotificationEvent push(Long userId, String title, String body) {
        return push(userId, title, body, Map.of());
    }

    public static NotificationEvent inApp(Long userId, String title, String body) {
        return inApp(userId, title, body, Map.of());
    }
}
