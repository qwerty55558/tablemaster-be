package com.mycompany.tablemaster.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 기반 Access Token 블랙리스트 서비스
 * 
 * 사용 시나리오:
 * 1. 로그아웃 시 현재 Access Token 무효화
 * 2. 전체 로그아웃 시 (모든 기기)
 * 3. 비밀번호 변경 시 기존 토큰 무효화
 * 4. 보안 이상 감지 시 즉시 차단
 * 
 * 블랙리스트 TTL은 Access Token의 남은 만료시간과 동일하게 설정
 * → 토큰이 자연 만료되면 블랙리스트에서도 자동 삭제 (메모리 효율)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * Access Token을 블랙리스트에 등록
     * 
     * @param jti JWT ID
     * @param remainingExpirationMs 남은 만료시간 (밀리초)
     */
    public void blacklist(String jti, long remainingExpirationMs) {
        if (jti == null || jti.isEmpty()) {
            log.warn("Cannot blacklist token: jti is null or empty");
            return;
        }

        if (remainingExpirationMs <= 0) {
            log.debug("Token already expired, no need to blacklist: jti={}", jti);
            return;
        }

        String key = BLACKLIST_PREFIX + jti;
        // 값은 블랙리스트 등록 시간 (디버깅용)
        String value = String.valueOf(System.currentTimeMillis());
        
        redisTemplate.opsForValue().set(key, value, Duration.ofMillis(remainingExpirationMs));
        log.info("Token blacklisted: jti={}, ttl={}ms", jti, remainingExpirationMs);
    }

    /**
     * Access Token이 블랙리스트에 있는지 확인
     * 
     * @param jti JWT ID
     * @return 블랙리스트 여부 (true = 차단됨)
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }

        String key = BLACKLIST_PREFIX + jti;
        Boolean exists = redisTemplate.hasKey(key);
        
        if (Boolean.TRUE.equals(exists)) {
            log.debug("Token is blacklisted: jti={}", jti);
            return true;
        }
        
        return false;
    }

    /**
     * 블랙리스트에서 토큰 제거 (관리자용, 일반적으로 사용 안 함)
     */
    public void removeFromBlacklist(String jti) {
        if (jti == null || jti.isEmpty()) {
            return;
        }

        String key = BLACKLIST_PREFIX + jti;
        Boolean deleted = redisTemplate.delete(key);
        
        if (Boolean.TRUE.equals(deleted)) {
            log.info("Token removed from blacklist: jti={}", jti);
        }
    }

    /**
     * 블랙리스트 남은 TTL 조회 (초)
     */
    public Long getBlacklistTtl(String jti) {
        if (jti == null || jti.isEmpty()) {
            return null;
        }

        String key = BLACKLIST_PREFIX + jti;
        return redisTemplate.getExpire(key);
    }
}
