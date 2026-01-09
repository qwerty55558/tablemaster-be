package com.mycompany.tablemaster.security;

import com.mycompany.tablemaster.config.properties.JwtProperties;
import com.mycompany.tablemaster.entity.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    protected void init() {
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(Long userId, String email, String name, Set<Role> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        List<String> roleNames = roles.stream()
                .map(Role::name)
                .toList();

        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("name", name)
                .claim("roles", roleNames)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 디바이스용 Access Token 생성
     */
    public String createDeviceAccessToken(Long id, String deviceId, String deviceName) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(id))
                .claim("deviceId", deviceId)
                .claim("deviceName", deviceName)
                .claim("roles", List.of("ROLE_DEVICE"))
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 디바이스용 Refresh Token 생성
     */
    public String createDeviceRefreshToken(Long id, String deviceId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim("deviceId", deviceId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 토큰에서 Claims 추출
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    /**
     * 토큰에서 이메일 추출
     */
    public String getEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    /**
     * 토큰에서 이름 추출
     */
    public String getName(String token) {
        return getClaims(token).get("name", String.class);
    }

    /**
     * 토큰에서 역할 추출
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return getClaims(token).get("roles", List.class);
    }

    /**
     * 토큰 타입 확인 (access / refresh)
     */
    public String getTokenType(String token) {
        return getClaims(token).get("type", String.class);
    }

    /**
     * 토큰에서 JWT ID(jti) 추출
     */
    public String getJti(String token) {
        return getClaims(token).getId();
    }

    /**
     * 토큰 만료 시간 추출
     */
    public Date getExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    /**
     * 토큰의 남은 만료 시간 (밀리초)
     */
    public long getRemainingExpiration(String token) {
        Date expiration = getExpiration(token);
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }

    /**
     * Access Token 여부 확인
     */
    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    /**
     * Refresh Token 여부 확인
     */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    /**
     * 디바이스 토큰 여부 확인
     */
    public boolean isDeviceToken(String token) {
        return getDeviceId(token) != null;
    }

    /**
     * 토큰에서 디바이스 ID 추출
     */
    public String getDeviceId(String token) {
        return getClaims(token).get("deviceId", String.class);
    }

    /**
     * Authentication 객체 생성 (Spring Security 연동)
     */
    public Authentication getAuthentication(String token) {
        Long userId = getUserId(token);
        List<String> roles = getRoles(token);

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }

    /**
     * Access Token 만료 시간 (초)
     */
    public long getAccessTokenExpirationInSeconds() {
        return jwtProperties.getAccessTokenExpirationInSeconds();
    }
}
