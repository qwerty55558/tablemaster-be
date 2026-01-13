package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.websocket.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 메시지 전송 서비스
 * Device와 User 모두 지원
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketSenderService {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;

    // ========== Device 경로별 전송 ==========

    /**
     * /queue/device - 디바이스 상태 (device_deleted, device_status)
     */
    public boolean sendDeviceStatus(String deviceId, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(deviceId, "/queue/device", payload);
            log.debug("Device status sent: deviceId={}", deviceId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send device status: deviceId={}", deviceId, e);
            return false;
        }
    }

    /**
     * /queue/tables - 테이블 목록 (tables_update)
     */
    public boolean sendTablesUpdate(String deviceId, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(deviceId, "/queue/tables", payload);
            log.debug("Tables update sent: deviceId={}", deviceId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send tables update: deviceId={}", deviceId, e);
            return false;
        }
    }

    /**
     * /queue/myTable - 내 테이블 (table_deleted, table_updated)
     */
    public boolean sendMyTableUpdate(String deviceId, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(deviceId, "/queue/myTable", payload);
            log.debug("MyTable update sent: deviceId={}", deviceId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send myTable update: deviceId={}", deviceId, e);
            return false;
        }
    }

    /**
     * /queue/chat - 채팅 메시지
     */
    public boolean sendChatToDevice(String deviceId, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(deviceId, "/queue/chat", payload);
            log.debug("Chat message sent to device: deviceId={}", deviceId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send chat message to device: deviceId={}", deviceId, e);
            return false;
        }
    }

    /**
     * /queue/notifications - 일반 알림 (토스트/팝업용)
     */
    public boolean sendToDevice(String deviceId, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(deviceId, "/queue/notifications", payload);
            log.debug("Notification sent to device: deviceId={}", deviceId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send notification to device: deviceId={}", deviceId, e);
            return false;
        }
    }

    /**
     * 모든 연결된 디바이스에 알림 전송
     */
    public void sendToAllDevices(Object payload) {
        for (String deviceId : sessionRegistry.getConnectedDevices()) {
            sendToDevice(deviceId, payload);
        }
    }

    /**
     * 모든 연결된 디바이스에 테이블 목록 전송
     */
    public void sendTablesUpdateToAll(Object payload) {
        for (String deviceId : sessionRegistry.getConnectedDevices()) {
            sendTablesUpdate(deviceId, payload);
        }
    }

    // ========== User 전송 ==========

    /**
     * 특정 User에게 알림 전송
     */
    public boolean sendToUser(Long userId, Object payload) {
        return sendToUser(String.valueOf(userId), payload);
    }

    /**
     * 특정 User에게 알림 전송
     */
    public boolean sendToUser(String userId, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload);
            log.debug("Notification sent to user: userId={}", userId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send notification to user: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 모든 연결된 User에게 알림 전송
     */
    public void sendToAllUsers(Object payload) {
        for (String userId : sessionRegistry.getConnectedUsers()) {
            sendToUser(userId, payload);
        }
    }

    // ========== Role 기반 전송 ==========

    /**
     * 특정 Role에게 브로드캐스트
     * 클라이언트는 /topic/role.{ROLE} 구독 필요
     */
    public void sendToRole(String role, Object payload) {
        String destination = "role." + role;
        broadcast(destination, payload);
        log.info("Broadcast sent to role: {}", role);
    }

    /**
     * ADMIN Role에게 브로드캐스트
     */
    public void sendToAdmins(Object payload) {
        sendToRole("ADMIN", payload);
    }

    /**
     * STAFF Role에게 브로드캐스트
     */
    public void sendToStaff(Object payload) {
        sendToRole("STAFF", payload);
    }

    // ========== 공통 ==========

    /**
     * 특정 토픽으로 브로드캐스트
     */
    public void broadcast(String destination, Object payload) {
        messagingTemplate.convertAndSend("/topic/" + destination, payload);
        log.debug("Broadcast sent to /topic/{}", destination);
    }

    /**
     * 전체 브로드캐스트 (Device + User 모두)
     */
    public void broadcastAll(Object payload) {
        broadcast("broadcast", payload);
    }

    // ========== 하위 호환성 ==========

    @Deprecated
    public boolean sendNotification(String deviceId, Object payload) {
        return sendToDevice(deviceId, payload);
    }

    @Deprecated
    public boolean sendChatMessage(String deviceId, Object payload) {
        return sendChatToDevice(deviceId, payload);
    }

    @Deprecated
    public void sendToAll(Object payload) {
        sendToAllDevices(payload);
    }
}
