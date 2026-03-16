package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room_histories")
@Getter
@Setter
@NoArgsConstructor
public class ChatRoomHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private String participants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomStatus status;

    @Column(name = "total_message_count", nullable = false)
    private Integer totalMessageCount;

    @Column(name = "gift_count", nullable = false)
    private Integer giftCount;

    @Column(name = "report_count", nullable = false)
    private Integer reportCount;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sanction_type")
    private SanctionType sanctionType;

    @Column(name = "sanction_reason")
    private String sanctionReason;

    @Column(name = "delete_reason", nullable = false)
    private String deleteReason;

    @PrePersist
    protected void onCreate() {
        deletedAt = LocalDateTime.now();
    }

    @Builder
    public ChatRoomHistory(Long roomId, String participants, ChatRoomStatus status,
                           Integer totalMessageCount, Integer giftCount, Integer reportCount,
                           LocalDateTime startedAt, LocalDateTime closedAt,
                           SanctionType sanctionType, String sanctionReason, String deleteReason) {
        this.roomId = roomId;
        this.participants = participants;
        this.status = status;
        this.totalMessageCount = totalMessageCount;
        this.giftCount = giftCount;
        this.reportCount = reportCount;
        this.startedAt = startedAt;
        this.closedAt = closedAt;
        this.sanctionType = sanctionType;
        this.sanctionReason = sanctionReason;
        this.deleteReason = deleteReason;
    }
}
