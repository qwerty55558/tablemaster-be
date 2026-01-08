package com.mycompany.tablemaster.entity;

/**
 * 사용자 역할 enum
 * - ROLE_ 접두사는 Spring Security 규약
 */
public enum Role {
    ROLE_USER,   // 일반 사용자
    ROLE_STAFF,  // 스태프 (기본값)
    ROLE_ADMIN   // 관리자
}
