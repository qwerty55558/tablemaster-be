package com.mycompany.tablemaster.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.Principal;

/**
 * REST API용 사용자 Principal
 */
@RequiredArgsConstructor
@Getter
public class UserAuthPrincipal implements Principal {

    private final Long userId;

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
