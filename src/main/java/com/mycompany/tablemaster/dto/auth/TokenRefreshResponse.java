package com.mycompany.tablemaster.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "토큰 갱신 응답")
public class TokenRefreshResponse {

    @Schema(description = "새로운 Access Token")
    private String accessToken;

    @Schema(description = "Access Token 만료 시간 (초)", example = "1800")
    private long expiresIn;

    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;

    public static TokenRefreshResponse of(String accessToken, long expiresIn) {
        return TokenRefreshResponse.builder()
                .accessToken(accessToken)
                .expiresIn(expiresIn)
                .tokenType("Bearer")
                .build();
    }
}
