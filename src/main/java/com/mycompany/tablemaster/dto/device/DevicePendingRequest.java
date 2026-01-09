package com.mycompany.tablemaster.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "디바이스 등록 요청")
public class DevicePendingRequest {

    @NotBlank(message = "Device ID는 필수입니다")
    @Schema(description = "디바이스 고유 ID", example = "a1b2c3d4e5f6")
    private String deviceId;

    @NotBlank(message = "App Secret은 필수입니다")
    @Schema(description = "앱 시크릿 키", example = "your-app-secret")
    private String appSecret;
}
