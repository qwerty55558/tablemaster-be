package com.mycompany.tablemaster.websocket;

import java.security.Principal;
import java.util.List;

/**
 * WebSocket Principal 공통 인터페이스
 * Device와 User 모두 지원
 */
public interface WebSocketPrincipal extends Principal {

    enum Type {
        DEVICE,
        USER
    }

    /**
     * Principal 타입 (DEVICE 또는 USER)
     */
    Type getType();

    /**
     * 식별자 (deviceId 또는 userId)
     */
    String getId();

    /**
     * 역할 목록 (User만 해당, Device는 빈 리스트)
     */
    List<String> getRoles();
}
