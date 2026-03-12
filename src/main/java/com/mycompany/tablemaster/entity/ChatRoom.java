package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomStatus status = ChatRoomStatus.ACTIVE;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "total_message_count", nullable = false)
    private Integer totalMessageCount = 0;

    @Column(name = "gift_count", nullable = false)
    private Integer giftCount = 0;

    @Column(name = "report_count", nullable = false)
    private Integer reportCount = 0;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }

    @Builder
    public ChatRoom(ChatRoomStatus status) {
        this.status = status != null ? status : ChatRoomStatus.ACTIVE;
        this.totalMessageCount = 0;
        this.giftCount = 0;
        this.reportCount = 0;
    }

    public void close() {
        this.status = ChatRoomStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public void sanction() {
        this.status = ChatRoomStatus.SANCTIONED;
        this.closedAt = LocalDateTime.now();
    }

    public void incrementMessageCount() {
        this.totalMessageCount++;
    }

    public void incrementGiftCount() {
        this.giftCount++;
    }

    public void incrementReportCount() {
        this.reportCount++;
    }
}
