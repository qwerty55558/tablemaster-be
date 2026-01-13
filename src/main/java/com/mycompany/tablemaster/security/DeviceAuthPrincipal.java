package com.mycompany.tablemaster.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.Principal;

/**
 * REST API용 디바이스 Principal
 */
@RequiredArgsConstructor
@Getter
public class DeviceAuthPrincipal implements Principal {

    private final String deviceId;

    @Override
    public String getName() {
        return deviceId;
    }
}
