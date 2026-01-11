package com.mycompany.tablemaster.websocket;

import com.mycompany.tablemaster.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket 세션 토큰 검증기
 * 주기적으로 연결된 세션들의 토큰 유효성을 검증하고
 * 만료되거나 블랙리스트에 있는 토큰의 세션을 강제 종료
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketTokenValidator {

    private final WebSocketSessionRegistry sessionRegistry;
    private final TokenBlacklistService tokenBlacklistService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 1분마다 연결된 세션들의 토큰 검증
     */
    @Scheduled(fixedRate = 60000)
    public void validateConnectedSessions() {
        validateDeviceSessions();
        validateUserSessions();
    }

    private void validateDeviceSessions() {
        Map<String, WebSocketSessionRegistry.SessionInfo> sessions = sessionRegistry.getDeviceSessions();

        for (Map.Entry<String, WebSocketSessionRegistry.SessionInfo> entry : sessions.entrySet()) {
            String deviceId = entry.getKey();
            WebSocketSessionRegistry.SessionInfo info = entry.getValue();

            if (isTokenInvalid(info)) {
                disconnectDevice(deviceId, info);
            }
        }
    }

    private void validateUserSessions() {
        Map<String, WebSocketSessionRegistry.SessionInfo> sessions = sessionRegistry.getUserSessions();

        for (Map.Entry<String, WebSocketSessionRegistry.SessionInfo> entry : sessions.entrySet()) {
            String userId = entry.getKey();
            WebSocketSessionRegistry.SessionInfo info = entry.getValue();

            if (isTokenInvalid(info)) {
                disconnectUser(userId, info);
            }
        }
    }

    private boolean isTokenInvalid(WebSocketSessionRegistry.SessionInfo info) {
        // 1. 토큰 만료 체크
        if (info.getExpiresAt() != null && Instant.now().isAfter(info.getExpiresAt())) {
            return true;
        }

        // 2. 블랙리스트 체크
        if (info.getJti() != null && tokenBlacklistService.isBlacklisted(info.getJti())) {
            return true;
        }

        return false;
    }

    private void disconnectDevice(String deviceId, WebSocketSessionRegistry.SessionInfo info) {
        log.info("Disconnecting device due to invalid token: deviceId={}, jti={}, expiresAt={}",
                deviceId, info.getJti(), info.getExpiresAt());

        // 클라이언트에게 세션 만료 알림 전송
        try {
            messagingTemplate.convertAndSendToUser(
                    deviceId,
                    "/queue/session",
                    Map.of(
                            "type", "SESSION_EXPIRED",
                            "message", "Your session has expired. Please reconnect.",
                            "timestamp", Instant.now().toString()
                    )
            );
        } catch (Exception e) {
            log.warn("Failed to send session expired message to device: {}", deviceId, e);
        }

        // 세션 레지스트리에서 제거
        sessionRegistry.removeDeviceSession(deviceId);
    }

    private void disconnectUser(String userId, WebSocketSessionRegistry.SessionInfo info) {
        log.info("Disconnecting user due to invalid token: userId={}, jti={}, expiresAt={}",
                userId, info.getJti(), info.getExpiresAt());

        // 클라이언트에게 세션 만료 알림 전송
        try {
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/session",
                    Map.of(
                            "type", "SESSION_EXPIRED",
                            "message", "Your session has expired. Please reconnect.",
                            "timestamp", Instant.now().toString()
                    )
            );
        } catch (Exception e) {
            log.warn("Failed to send session expired message to user: {}", userId, e);
        }

        // 세션 레지스트리에서 제거
        sessionRegistry.removeUserSession(userId);
    }
}
