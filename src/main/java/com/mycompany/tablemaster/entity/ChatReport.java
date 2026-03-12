package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_reports")
@Getter
@Setter
@NoArgsConstructor
public class ChatReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "reporter_device_id", nullable = false)
    private String reporterDeviceId;

    @Column(name = "reported_device_id", nullable = false)
    private String reportedDeviceId;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatReportStatus status = ChatReportStatus.PENDING;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public ChatReport(ChatRoom chatRoom, String reporterDeviceId, String reportedDeviceId,
                      String reason) {
        this.chatRoom = chatRoom;
        this.reporterDeviceId = reporterDeviceId;
        this.reportedDeviceId = reportedDeviceId;
        this.reason = reason;
        this.status = ChatReportStatus.PENDING;
    }

    public void review(Long userId, ChatReportStatus status) {
        this.reviewedBy = userId;
        this.status = status;
    }
}
