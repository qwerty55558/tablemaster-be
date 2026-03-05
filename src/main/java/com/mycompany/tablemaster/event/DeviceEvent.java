package com.mycompany.tablemaster.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record DeviceEvent(
    String deviceId,
    DeviceEventType type,
    LocalDateTime timestamp
) implements Serializable {

    public static DeviceEvent registrationExpired(String deviceId) {
        return new DeviceEvent(deviceId, DeviceEventType.DEVICE_REGISTRATION_EXPIRED, LocalDateTime.now());
    }
}
