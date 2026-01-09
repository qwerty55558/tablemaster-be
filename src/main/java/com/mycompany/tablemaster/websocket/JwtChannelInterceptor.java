package com.mycompany.tablemaster.websocket;

import com.mycompany.tablemaster.security.JwtTokenProvider;
import com.mycompany.tablemaster.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.List;

/**
 * STOMP 메시지 인터셉터
 * CONNECT 시 JWT 토큰 검증 및 Device/User 구분 처리
 */
@Slf4j
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        }

        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            handleDisconnect(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);

        if (token == null) {
            throw new MessagingException("Authorization token is required");
        }

        if (!jwtTokenProvider.validateToken(token)) {
            throw new MessagingException("Invalid or expired token");
        }

        // 블랙리스트 확인
        String jti = jwtTokenProvider.getJti(token);
        if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
            throw new MessagingException("Token has been revoked");
        }

        // Device 토큰 vs User 토큰 구분
        String deviceId = jwtTokenProvider.getDeviceId(token);

        if (deviceId != null) {
            // Device 토큰
            handleDeviceConnect(accessor, deviceId);
        } else {
            // User 토큰
            handleUserConnect(accessor, token);
        }
    }

    private void handleDeviceConnect(StompHeaderAccessor accessor, String deviceId) {
        // Principal 설정
        accessor.setUser(new DevicePrincipal(deviceId));

        // 세션 속성 저장
        if (accessor.getSessionAttributes() != null) {
            accessor.getSessionAttributes().put("type", "DEVICE");
            accessor.getSessionAttributes().put("id", deviceId);
        }

        log.info("WebSocket CONNECT [DEVICE]: deviceId={}", deviceId);
    }

    private void handleUserConnect(StompHeaderAccessor accessor, String token) {
        Long userId = jwtTokenProvider.getUserId(token);
        List<String> roles = jwtTokenProvider.getRoles(token);

        // Principal 설정
        accessor.setUser(new UserPrincipal(userId, roles));

        // 세션 속성 저장
        if (accessor.getSessionAttributes() != null) {
            accessor.getSessionAttributes().put("type", "USER");
            accessor.getSessionAttributes().put("id", String.valueOf(userId));
            accessor.getSessionAttributes().put("roles", roles);
        }

        log.info("WebSocket CONNECT [USER]: userId={}, roles={}", userId, roles);
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        if (accessor.getSessionAttributes() == null) {
            return;
        }

        String type = (String) accessor.getSessionAttributes().get("type");
        String id = (String) accessor.getSessionAttributes().get("id");

        if (type == null || id == null) {
            return;
        }

        if ("DEVICE".equals(type)) {
            sessionRegistry.removeDeviceSession(id);
            log.info("WebSocket DISCONNECT [DEVICE]: deviceId={}", id);
        } else if ("USER".equals(type)) {
            sessionRegistry.removeUserSession(id);
            log.info("WebSocket DISCONNECT [USER]: userId={}", id);
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // 1. Authorization 헤더에서 추출
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. token 헤더에서 추출 (SockJS 호환)
        String tokenHeader = accessor.getFirstNativeHeader("token");
        if (tokenHeader != null) {
            return tokenHeader;
        }

        return null;
    }
}
