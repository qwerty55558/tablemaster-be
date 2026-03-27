package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_moderation_histories")
@Getter
@Setter
@NoArgsConstructor
public class ChatModerationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "table_names", nullable = false)
    private String tableNames;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ChatModerationActionType actionType;

    @Column(length = 500)
    private String reason;

    @Column(name = "action_detail", columnDefinition = "TEXT")
    private String actionDetail;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "processed_by_user_id")
    private Long processedByUserId;

    @Column(name = "processed_by_name")
    private String processedByName;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @PrePersist
    protected void onCreate() {
        processedAt = LocalDateTime.now();
    }

    @Builder
    public ChatModerationHistory(Long chatRoomId, String tableNames, ChatModerationActionType actionType,
                                 String reason, String actionDetail, Long reportId,
                                 Long processedByUserId, String processedByName, String payload) {
        this.chatRoomId = chatRoomId;
        this.tableNames = tableNames;
        this.actionType = actionType;
        this.reason = reason;
        this.actionDetail = actionDetail;
        this.reportId = reportId;
        this.processedByUserId = processedByUserId;
        this.processedByName = processedByName;
        this.payload = payload;
    }
}
