package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "forbidden_words")
@Getter
@Setter
@NoArgsConstructor
public class ForbiddenWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String word;

    @Column(length = 500)
    private String reason;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public ForbiddenWord(String word, String reason, Boolean isActive, Long createdByUserId, String createdByName) {
        this.word = word;
        this.reason = reason;
        this.isActive = isActive != null ? isActive : true;
        this.createdByUserId = createdByUserId;
        this.createdByName = createdByName;
    }
}
