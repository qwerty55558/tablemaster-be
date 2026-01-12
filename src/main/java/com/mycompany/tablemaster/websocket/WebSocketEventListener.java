package com.mycompany.tablemaster.websocket;

import com.mycompany.tablemaster.service.NotificationService;
import com.mycompany.tablemaster.service.WebSocketSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket 연결/해제 이벤트 리스너
 * Device와 User 모두 지원
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final WebSocketSessionRegistry sessionRegistry;
    private final NotificationService notificationService;
    private final WebSocketSenderService webSocketSenderService;

    /**
     * 세션 연결 완료 이벤트
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        log.info("SessionConnectedEvent received: sessionId={}, sessionAttributes={}",
                sessionId, accessor.getSessionAttributes());

        if (sessionId == null || accessor.getSessionAttributes() == null) {
            log.warn("SessionConnectedEvent skipped: sessionId={}, hasAttributes={}",
                    sessionId, accessor.getSessionAttributes() != null);
            return;
        }

        String type = (String) accessor.getSessionAttributes().get("type");
        String id = (String) accessor.getSessionAttributes().get("id");

        if (type == null || id == null) {
            log.warn("SessionConnectedEvent skipped: type={}, id={}", type, id);
            return;
        }

        Map<String, Object> attrs = accessor.getSessionAttributes();

        if ("DEVICE".equals(type)) {
            handleDeviceConnected(id, sessionId, attrs);
        } else if ("USER".equals(type)) {
            handleUserConnected(id, sessionId, attrs);
        }
    }

    private void handleDeviceConnected(String deviceId, String sessionId, Map<String, Object> attrs) {
        String jti = (String) attrs.get("jti");
        Instant expiresAt = (Instant) attrs.get("expiresAt");

        WebSocketSessionRegistry.SessionInfo info =
                new WebSocketSessionRegistry.SessionInfo(sessionId, jti, expiresAt);
        sessionRegistry.registerDeviceSession(deviceId, info);
        log.info("Device session connected: deviceId={}, sessionId={}", deviceId, sessionId);

        // 재접속 시 미전달 알림 전송
        notificationService.sendUndeliveredNotifications(deviceId);

        // Admin에게 Device 연결 알림
        webSocketSenderService.sendToAdmins(Map.of(
                "type", "DEVICE_CONNECTED",
                "deviceId", deviceId,
                "timestamp", Instant.now().toString()
        ));
    }

    private void handleUserConnected(String userId, String sessionId, Map<String, Object> attrs) {
        String jti = (String) attrs.get("jti");
        Instant expiresAt = (Instant) attrs.get("expiresAt");

        WebSocketSessionRegistry.SessionInfo info =
                new WebSocketSessionRegistry.SessionInfo(sessionId, jti, expiresAt);
        sessionRegistry.registerUserSession(userId, info);
        log.info("User session connected: userId={}, sessionId={}", userId, sessionId);

        // User도 필요시 미전달 알림 전송 가능 (현재는 생략)
    }

    /**
     * 세션 해제 이벤트
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

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
            log.info("Device session disconnected: deviceId={}", id);

            // Admin에게 Device 해제 알림
            webSocketSenderService.sendToAdmins(Map.of(
                    "type", "DEVICE_DISCONNECTED",
                    "deviceId", id,
                    "timestamp", Instant.now().toString()
            ));
        } else if ("USER".equals(type)) {
            sessionRegistry.removeUserSession(id);
            log.info("User session disconnected: userId={}", id);
        }
    }
}
