package com.mycompany.tablemaster.dto.auth;

import com.mycompany.tablemaster.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SignUpResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private LocalDateTime createdAt;

    public static SignUpResponse from(User user) {
        return SignUpResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
