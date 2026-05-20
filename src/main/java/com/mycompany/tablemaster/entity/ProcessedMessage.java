package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 메시지 멱등 처리 이력. 같은 idempotency_key 가 두 번 도착하면 두 번째는 스킵.
 * key 는 payload SHA-256(또는 producer가 부여한 messageId)로 산정.
 */
@Entity
@Table(
        name = "processed_messages",
        uniqueConstraints = @UniqueConstraint(name = "uk_processed_messages_key", columnNames = "idempotency_key")
)
@Getter
@NoArgsConstructor
public class ProcessedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "queue_name", length = 255)
    private String queueName;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }

    @Builder
    public ProcessedMessage(String idempotencyKey, String queueName) {
        this.idempotencyKey = idempotencyKey;
        this.queueName = queueName;
    }
}
