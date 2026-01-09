package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tables")
@Getter
@Setter
@NoArgsConstructor
public class TableEntity {

    @Id
    private String id;  // "A1", "B2" 등

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableStatus status = TableStatus.AVAILABLE;

    private String location;        // 지역

    private Integer guestCount;     // 총 인원

    private Integer femaleCount;    // 여성 인원

    private Integer maleCount;      // 남성 인원

    private String deviceId;        // 연결된 디바이스 ID

    @Column(nullable = false)
    private Boolean isChatting = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public TableEntity(String id, String name, TableStatus status, String location,
                       Integer guestCount, Integer femaleCount, Integer maleCount,
                       String deviceId, Boolean isChatting) {
        this.id = id;
        this.name = name;
        this.status = status != null ? status : TableStatus.AVAILABLE;
        this.location = location;
        this.guestCount = guestCount;
        this.femaleCount = femaleCount;
        this.maleCount = maleCount;
        this.deviceId = deviceId;
        this.isChatting = isChatting != null ? isChatting : false;
    }

    /**
     * 테이블 설정 (입장 시)
     */
    public void setup(String location, Integer guestCount, Integer femaleCount, Integer maleCount, String deviceId) {
        this.location = location;
        this.guestCount = guestCount;
        this.femaleCount = femaleCount;
        this.maleCount = maleCount;
        this.deviceId = deviceId;
        this.status = TableStatus.OCCUPIED;
        this.isChatting = false;
    }

    /**
     * 테이블 초기화 (리셋)
     */
    public void reset() {
        this.location = null;
        this.guestCount = null;
        this.femaleCount = null;
        this.maleCount = null;
        this.deviceId = null;
        this.status = TableStatus.AVAILABLE;
        this.isChatting = false;
    }

    /**
     * 채팅 상태 변경
     */
    public void startChatting() {
        this.status = TableStatus.CHATTING;
        this.isChatting = true;
    }

    public void endChatting() {
        this.status = TableStatus.OCCUPIED;
        this.isChatting = false;
    }
}
