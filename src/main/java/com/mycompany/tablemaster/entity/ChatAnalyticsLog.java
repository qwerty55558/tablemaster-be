package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_analytics_logs")
@Getter
@Setter
@NoArgsConstructor
public class ChatAnalyticsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private ChatAnalyticsEventType eventType;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "actor_device_id")
    private String actorDeviceId;

    @Column(name = "partner_device_id")
    private String partnerDeviceId;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "message_type")
    private String messageType;

    @Column(name = "total_message_count")
    private Integer totalMessageCount;

    @Column(name = "gift_count")
    private Integer giftCount;

    @Column(name = "report_count")
    private Integer reportCount;

    @Column(length = 255)
    private String reason;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        loggedAt = LocalDateTime.now();
    }

    @Builder
    public ChatAnalyticsLog(ChatAnalyticsEventType eventType, Long chatRoomId, String actorDeviceId,
                            String partnerDeviceId, Long messageId, Long reportId, String messageType,
                            Integer totalMessageCount, Integer giftCount, Integer reportCount, String reason) {
        this.eventType = eventType;
        this.chatRoomId = chatRoomId;
        this.actorDeviceId = actorDeviceId;
        this.partnerDeviceId = partnerDeviceId;
        this.messageId = messageId;
        this.reportId = reportId;
        this.messageType = messageType;
        this.totalMessageCount = totalMessageCount;
        this.giftCount = giftCount;
        this.reportCount = reportCount;
        this.reason = reason;
    }
}
