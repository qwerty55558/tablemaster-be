package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "email_notification_enabled", nullable = false)
    private Boolean emailNotificationEnabled = true;

    @Column(name = "push_notification_enabled", nullable = false)
    private Boolean pushNotificationEnabled = true;

    @Column(name = "marketing_notification_enabled", nullable = false)
    private Boolean marketingNotificationEnabled = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserTermsAgreement> termsAgreements = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public User(String email, String password, String name, String phone,
                String profileImageUrl, Boolean emailNotificationEnabled,
                Boolean pushNotificationEnabled, Boolean marketingNotificationEnabled) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
        this.emailNotificationEnabled = emailNotificationEnabled != null ? emailNotificationEnabled : true;
        this.pushNotificationEnabled = pushNotificationEnabled != null ? pushNotificationEnabled : true;
        this.marketingNotificationEnabled = marketingNotificationEnabled != null ? marketingNotificationEnabled : false;
        this.roles.add(Role.ROLE_STAFF);  // 기본 역할: STAFF
    }

    /**
     * 역할 추가
     */
    public void addRole(Role role) {
        this.roles.add(role);
    }

    /**
     * 역할 제거
     */
    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    /**
     * 특정 역할 보유 여부
     */
    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }

    /**
     * 비밀번호 변경
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateProfile(String name, String phone, String profileImageUrl) {
        if (name != null) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public void updateNotificationSettings(Boolean emailNotificationEnabled,
                                           Boolean pushNotificationEnabled,
                                           Boolean marketingNotificationEnabled) {
        this.emailNotificationEnabled = emailNotificationEnabled;
        this.pushNotificationEnabled = pushNotificationEnabled;
        this.marketingNotificationEnabled = marketingNotificationEnabled;
    }
}
