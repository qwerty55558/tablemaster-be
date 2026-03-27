package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_monitor_logs")
@Getter
@Setter
@NoArgsConstructor
public class ChatMonitorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private ChatMonitorLogType eventType;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_device_id")
    private String actorDeviceId;

    @Column(name = "actor_table_name")
    private String actorTableName;

    @Column(name = "target_device_id")
    private String targetDeviceId;

    @Column(name = "target_table_name")
    private String targetTableName;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 500)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        loggedAt = LocalDateTime.now();
    }

    @Builder
    public ChatMonitorLog(ChatMonitorLogType eventType, Long chatRoomId, Long messageId,
                          Long reportId, Long actorUserId, String actorDeviceId,
                          String actorTableName, String targetDeviceId, String targetTableName,
                          String content, String reason, String payload) {
        this.eventType = eventType;
        this.chatRoomId = chatRoomId;
        this.messageId = messageId;
        this.reportId = reportId;
        this.actorUserId = actorUserId;
        this.actorDeviceId = actorDeviceId;
        this.actorTableName = actorTableName;
        this.targetDeviceId = targetDeviceId;
        this.targetTableName = targetTableName;
        this.content = content;
        this.reason = reason;
        this.payload = payload;
    }
}
