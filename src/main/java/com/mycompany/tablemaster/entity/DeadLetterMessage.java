package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dead_letter_messages")
@Getter
@Setter
@NoArgsConstructor
public class DeadLetterMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_exchange", length = 255)
    private String originalExchange;

    @Column(name = "original_routing_key", length = 255)
    private String originalRoutingKey;

    @Column(name = "original_queue", length = 255)
    private String originalQueue;

    @Column(name = "death_reason", length = 100)
    private String deathReason;

    @Column(name = "death_count")
    private Long deathCount;

    @Column(name = "first_failed_at")
    private LocalDateTime firstFailedAt;

    @Column(name = "message_id", length = 255)
    private String messageId;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Lob
    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;

    @Lob
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeadLetterMessageStatus status;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        loggedAt = LocalDateTime.now();
        if (status == null) {
            status = DeadLetterMessageStatus.PENDING;
        }
    }

    @Builder
    public DeadLetterMessage(String originalExchange, String originalRoutingKey, String originalQueue,
                              String deathReason, Long deathCount, LocalDateTime firstFailedAt,
                              String messageId, String contentType, String headers, String payload,
                              DeadLetterMessageStatus status) {
        this.originalExchange = originalExchange;
        this.originalRoutingKey = originalRoutingKey;
        this.originalQueue = originalQueue;
        this.deathReason = deathReason;
        this.deathCount = deathCount;
        this.firstFailedAt = firstFailedAt;
        this.messageId = messageId;
        this.contentType = contentType;
        this.headers = headers;
        this.payload = payload;
        this.status = status;
    }
}
