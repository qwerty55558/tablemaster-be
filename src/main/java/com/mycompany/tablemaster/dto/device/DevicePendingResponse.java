package com.mycompany.tablemaster.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "대기 중인 디바이스 정보")
public class DevicePendingResponse {

    @Schema(description = "디바이스 고유 ID", example = "a1b2c3d4e5f6")
    private String deviceId;

    @Schema(description = "요청 시간", example = "2024-01-09T10:00:00")
    private String requestedAt;

    @Schema(description = "남은 TTL (초)", example = "150")
    private Long ttl;
}
