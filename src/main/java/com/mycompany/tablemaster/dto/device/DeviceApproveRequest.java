package com.mycompany.tablemaster.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "디바이스 승인 요청")
public class DeviceApproveRequest {

    @NotBlank(message = "디바이스 이름은 필수입니다")
    @Size(max = 100, message = "디바이스 이름은 100자 이하로 입력해주세요")
    @Schema(description = "디바이스 이름", example = "1번 테이블")
    private String deviceName;
}
