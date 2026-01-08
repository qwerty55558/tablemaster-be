package com.mycompany.tablemaster.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "토큰 갱신 요청")
public class TokenRefreshRequest {

    @NotBlank(message = "Refresh Token을 입력해주세요")
    @Schema(description = "Refresh Token")
    private String refreshToken;
}
