package com.mycompany.tablemaster.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record TableEvent(
    String tableId,
    String deviceId,
    TableEventType type,
    LocalDateTime timestamp
) implements Serializable {

    public static TableEvent setup(String tableId, String deviceId) {
        return new TableEvent(tableId, deviceId, TableEventType.SETUP, LocalDateTime.now());
    }

    public static TableEvent reset(String tableId, String deviceId) {
        return new TableEvent(tableId, deviceId, TableEventType.RESET, LocalDateTime.now());
    }

    public static TableEvent updated(String tableId, String deviceId) {
        return new TableEvent(tableId, deviceId, TableEventType.UPDATED, LocalDateTime.now());
    }
}
