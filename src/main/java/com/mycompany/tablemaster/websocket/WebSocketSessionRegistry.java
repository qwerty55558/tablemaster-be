package com.mycompany.tablemaster.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 세션 레지스트리
 * Device/User 세션 매핑 관리 (단일 서버용)
 */
@Component
@Slf4j
public class WebSocketSessionRegistry {

    private final ConcurrentHashMap<String, String> deviceSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> userSessions = new ConcurrentHashMap<>();

    // ========== Device 세션 관리 ==========

    /**
     * Device 세션 등록
     */
    public void registerDeviceSession(String deviceId, String sessionId) {
        String oldSession = deviceSessions.put(deviceId, sessionId);
        if (oldSession != null) {
            log.info("Device reconnected, old session replaced: deviceId={}", deviceId);
        }
        log.debug("Device session registered: deviceId={}, sessionId={}", deviceId, sessionId);
    }

    /**
     * Device 세션 제거
     */
    public void removeDeviceSession(String deviceId) {
        deviceSessions.remove(deviceId);
        log.debug("Device session removed: deviceId={}", deviceId);
    }

    /**
     * Device 연결 여부 확인
     */
    public boolean isDeviceConnected(String deviceId) {
        return deviceSessions.containsKey(deviceId);
    }

    /**
     * 연결된 모든 디바이스 조회
     */
    public Set<String> getConnectedDevices() {
        return Collections.unmodifiableSet(deviceSessions.keySet());
    }

    // ========== User 세션 관리 ==========

    /**
     * User 세션 등록
     */
    public void registerUserSession(String userId, String sessionId) {
        String oldSession = userSessions.put(userId, sessionId);
        if (oldSession != null) {
            log.info("User reconnected, old session replaced: userId={}", userId);
        }
        log.debug("User session registered: userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * User 세션 제거
     */
    public void removeUserSession(String userId) {
        userSessions.remove(userId);
        log.debug("User session removed: userId={}", userId);
    }

    /**
     * User 연결 여부 확인
     */
    public boolean isUserConnected(String userId) {
        return userSessions.containsKey(userId);
    }

    /**
     * 연결된 모든 User 조회
     */
    public Set<String> getConnectedUsers() {
        return Collections.unmodifiableSet(userSessions.keySet());
    }

    // ========== 공통 ==========

    /**
     * 연결된 총 수 (Device + User)
     */
    public int getTotalConnectedCount() {
        return deviceSessions.size() + userSessions.size();
    }

    /**
     * 연결된 Device 수
     */
    public int getConnectedDeviceCount() {
        return deviceSessions.size();
    }

    /**
     * 연결된 User 수
     */
    public int getConnectedUserCount() {
        return userSessions.size();
    }

    // ========== 하위 호환성 (기존 메서드) ==========

    @Deprecated
    public void registerSession(String deviceId, String sessionId) {
        registerDeviceSession(deviceId, sessionId);
    }

    @Deprecated
    public void removeSession(String deviceId) {
        removeDeviceSession(deviceId);
    }

    @Deprecated
    public boolean isConnected(String deviceId) {
        return isDeviceConnected(deviceId);
    }
}
