package com.mycompany.tablemaster.websocket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Device용 WebSocket Principal
 */
@RequiredArgsConstructor
@Getter
public class DevicePrincipal implements WebSocketPrincipal {

    private final String deviceId;

    @Override
    public String getName() {
        return deviceId;
    }

    @Override
    public Type getType() {
        return Type.DEVICE;
    }

    @Override
    public String getId() {
        return deviceId;
    }

    @Override
    public List<String> getRoles() {
        return Collections.singletonList("ROLE_DEVICE");
    }
}
