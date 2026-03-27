package com.mycompany.tablemaster.dto.auth;

import com.mycompany.tablemaster.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class ProfileResponse {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private String profileImageUrl;
    private Set<String> roles;
    private Boolean emailNotificationEnabled;
    private Boolean pushNotificationEnabled;
    private Boolean marketingNotificationEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProfileResponse from(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .profileImageUrl(user.getProfileImageUrl())
                .roles(user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()))
                .emailNotificationEnabled(user.getEmailNotificationEnabled())
                .pushNotificationEnabled(user.getPushNotificationEnabled())
                .marketingNotificationEnabled(user.getMarketingNotificationEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
