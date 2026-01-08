package com.mycompany.tablemaster.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 기반 Rate Limiting 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOGIN_ATTEMPT_PREFIX = "rate:login:";
    private static final String REFRESH_ATTEMPT_PREFIX = "rate:refresh:";

    /**
     * 로그인 시도 제한 체크 (IP 기준)
     * @param ip 클라이언트 IP
     * @param maxAttempts 최대 시도 횟수
     * @param windowMinutes 시간 윈도우 (분)
     * @return 허용 여부
     */
    public boolean isLoginAllowed(String ip, int maxAttempts, int windowMinutes) {
        String key = LOGIN_ATTEMPT_PREFIX + ip;
        return isAllowed(key, maxAttempts, windowMinutes);
    }

    /**
     * 로그인 시도 기록
     */
    public void recordLoginAttempt(String ip, int windowMinutes) {
        String key = LOGIN_ATTEMPT_PREFIX + ip;
        recordAttempt(key, windowMinutes);
    }

    /**
     * 로그인 성공 시 카운터 리셋
     */
    public void resetLoginAttempts(String ip) {
        String key = LOGIN_ATTEMPT_PREFIX + ip;
        redisTemplate.delete(key);
    }

    /**
     * 토큰 갱신 제한 체크 (토큰 기준)
     */
    public boolean isRefreshAllowed(String tokenHash, int maxAttempts, int windowMinutes) {
        String key = REFRESH_ATTEMPT_PREFIX + tokenHash;
        return isAllowed(key, maxAttempts, windowMinutes);
    }

    /**
     * 토큰 갱신 시도 기록
     */
    public void recordRefreshAttempt(String tokenHash, int windowMinutes) {
        String key = REFRESH_ATTEMPT_PREFIX + tokenHash;
        recordAttempt(key, windowMinutes);
    }

    /**
     * 남은 시도 횟수 조회
     */
    public int getRemainingAttempts(String ip, int maxAttempts) {
        String key = LOGIN_ATTEMPT_PREFIX + ip;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return maxAttempts;
        }
        int current = Integer.parseInt(value);
        return Math.max(0, maxAttempts - current);
    }

    private boolean isAllowed(String key, int maxAttempts, int windowMinutes) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return true;
        }
        int attempts = Integer.parseInt(value);
        return attempts < maxAttempts;
    }

    private void recordAttempt(String key, int windowMinutes) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 첫 번째 시도일 때만 만료 시간 설정
            redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
        }
    }
}
