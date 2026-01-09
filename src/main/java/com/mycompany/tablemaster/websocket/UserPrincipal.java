package com.mycompany.tablemaster.websocket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * User용 WebSocket Principal
 */
@RequiredArgsConstructor
@Getter
public class UserPrincipal implements WebSocketPrincipal {

    private final Long userId;
    private final List<String> roles;

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    @Override
    public Type getType() {
        return Type.USER;
    }

    @Override
    public String getId() {
        return String.valueOf(userId);
    }

    /**
     * 특정 역할 보유 여부
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * ADMIN 역할 여부
     */
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * STAFF 역할 여부
     */
    public boolean isStaff() {
        return hasRole("ROLE_STAFF") || hasRole("ROLE_ADMIN");
    }
}
