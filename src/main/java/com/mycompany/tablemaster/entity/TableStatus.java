package com.mycompany.tablemaster.entity;

/**
 * 테이블 상태 enum
 */
public enum TableStatus {
    AVAILABLE,   // 이용 가능
    OCCUPIED,    // 사용 중
    RESERVED,    // 예약됨
    CHATTING,    // 채팅 중
    INACTIVE     // 디바이스 연결 끊김 (임시 비활성)
}
