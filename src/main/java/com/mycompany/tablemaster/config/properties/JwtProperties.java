package com.mycompany.tablemaster.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret;
    private long accessTokenExpiration;   // 30분 (ms)
    private long refreshTokenExpiration;  // 14일 (ms)

    /**
     * Access Token 만료 시간 (초 단위)
     */
    public long getAccessTokenExpirationInSeconds() {
        return accessTokenExpiration / 1000;
    }

    /**
     * Refresh Token 만료 시간 (초 단위)
     */
    public long getRefreshTokenExpirationInSeconds() {
        return refreshTokenExpiration / 1000;
    }
}
