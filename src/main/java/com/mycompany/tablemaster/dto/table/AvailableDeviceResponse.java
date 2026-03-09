package com.mycompany.tablemaster.dto.table;

import com.mycompany.tablemaster.entity.DeviceWhitelist;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "빈 디바이스 (테이블 미등록)")
public class AvailableDeviceResponse {

    @Schema(description = "디바이스 ID", example = "device-001")
    private String deviceId;

    @Schema(description = "디바이스 이름", example = "1번 태블릿")
    private String deviceName;

    public static AvailableDeviceResponse from(DeviceWhitelist device) {
        return AvailableDeviceResponse.builder()
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .build();
    }
}
