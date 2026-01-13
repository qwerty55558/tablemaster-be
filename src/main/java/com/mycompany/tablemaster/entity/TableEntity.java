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
    private String id;  // deviceId (디바이스 고유 식별자 = PK)

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableStatus status = TableStatus.AVAILABLE;

    private String location;        // 지역

    private Integer guestCount;     // 총 인원

    private Integer femaleCount;    // 여성 인원

    private Integer maleCount;      // 남성 인원

    @Column
    private Long revenue = 0L;      // 매상

    @Column(nullable = false)
    private Boolean isChatting = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private TableStatus previousStatus;  // 비활성화 전 상태 저장

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
                       Long revenue, Boolean isChatting, TableStatus previousStatus) {
        this.id = id;
        this.name = name;
        this.status = status != null ? status : TableStatus.AVAILABLE;
        this.location = location;
        this.guestCount = guestCount;
        this.femaleCount = femaleCount;
        this.maleCount = maleCount;
        this.revenue = revenue != null ? revenue : 0L;
        this.isChatting = isChatting != null ? isChatting : false;
        this.previousStatus = previousStatus;
    }

    /**
     * 테이블 설정 (입장 시)
     */
    public void setup(String name, String location, Integer guestCount, Integer femaleCount, Integer maleCount) {
        this.name = name;
        this.location = location;
        this.guestCount = guestCount;
        this.femaleCount = femaleCount;
        this.maleCount = maleCount;
        this.status = TableStatus.OCCUPIED;
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

    /**
     * 디바이스 연결 해제 시 비활성화
     */
    public void deactivate() {
        if (this.status != TableStatus.INACTIVE) {
            this.previousStatus = this.status;
            this.status = TableStatus.INACTIVE;
        }
    }

    /**
     * 디바이스 재연결 시 활성화 (이전 상태 복원)
     */
    public void activate() {
        if (this.status == TableStatus.INACTIVE && this.previousStatus != null) {
            this.status = this.previousStatus;
            this.previousStatus = null;
        } else if (this.status == TableStatus.INACTIVE) {
            this.status = TableStatus.OCCUPIED;
        }
    }

    /**
     * 활성 상태인지 확인 (INACTIVE가 아닌 경우)
     */
    public boolean isActive() {
        return this.status != TableStatus.INACTIVE;
    }
}
