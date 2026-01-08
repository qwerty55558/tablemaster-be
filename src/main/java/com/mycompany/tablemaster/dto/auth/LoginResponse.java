package com.mycompany.tablemaster.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "로그인 응답")
public class LoginResponse {

    @Schema(description = "Access Token")
    private String accessToken;

    @Schema(description = "Refresh Token")
    private String refreshToken;

    @Schema(description = "Access Token 만료 시간 (초)", example = "1800")
    private long expiresIn;

    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;

    public static LoginResponse of(String accessToken, String refreshToken, long expiresIn) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .tokenType("Bearer")
                .build();
    }
}
