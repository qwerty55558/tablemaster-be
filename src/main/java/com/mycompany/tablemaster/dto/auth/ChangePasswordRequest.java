package com.mycompany.tablemaster.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ChangePasswordRequest {

    @NotBlank(message = "현재 비밀번호를 입력해주세요")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호를 입력해주세요")
    private String newPassword;

    /**
     * true인 경우 다른 모든 기기에서도 로그아웃 (모든 토큰 무효화)
     * false인 경우 현재 기기의 토큰만 유지
     */
    private boolean logoutOtherDevices = true;
}
