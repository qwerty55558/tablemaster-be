package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "terms")
@Getter
@Setter
@NoArgsConstructor
public class Terms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TermsType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_active")
    private boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public Terms(TermsType type, String title, String content, String version, boolean required) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.version = version;
        this.required = required;
        this.isActive = true;
    }

    public enum TermsType {
        SERVICE,      // 서비스 이용 약관
        PRIVACY,      // 개인정보 처리 방침
        MARKETING     // 마케팅 정보 수신 동의
    }
}
