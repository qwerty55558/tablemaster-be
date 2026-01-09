package com.mycompany.tablemaster.dto.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.tablemaster.entity.Notification;
import com.mycompany.tablemaster.entity.NotificationCategory;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record NotificationDTO(
    Long id,
    String title,
    String body,
    NotificationCategory category,
    Map<String, Object> data,
    Boolean isRead,
    LocalDateTime createdAt
) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static NotificationDTO from(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .category(notification.getCategory())
                .data(parseData(notification.getData()))
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private static Map<String, Object> parseData(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
