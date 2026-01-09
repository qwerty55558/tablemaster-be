package com.mycompany.tablemaster.dto.device;

import com.mycompany.tablemaster.entity.DeviceWhitelist;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "디바이스 정보 응답")
public class DeviceResponse {

    @Schema(description = "디바이스 DB ID", example = "1")
    private Long id;

    @Schema(description = "디바이스 고유 ID", example = "a1b2c3d4e5f6")
    private String deviceId;

    @Schema(description = "디바이스 이름", example = "주방 태블릿")
    private String deviceName;

    @Schema(description = "활성화 여부", example = "true")
    private Boolean isActive;

    @Schema(description = "등록일시")
    private LocalDateTime createdAt;

    @Schema(description = "마지막 로그인 일시")
    private LocalDateTime lastLoginAt;

    public static DeviceResponse from(DeviceWhitelist device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .isActive(device.getIsActive())
                .createdAt(device.getCreatedAt())
                .lastLoginAt(device.getLastLoginAt())
                .build();
    }
}
