package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.auth.*;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.service.AuthService;
import com.mycompany.tablemaster.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    // Rate Limiting 설정
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOGIN_WINDOW_MINUTES = 1;
    private static final int MAX_REFRESH_ATTEMPTS = 10;
    private static final int REFRESH_WINDOW_MINUTES = 1;

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    @PostMapping("/check-email")
    @Operation(summary = "이메일 중복확인", description = "이메일 사용 가능 여부 확인 (실시간 debounce 요청용)")
    public ResponseEntity<EmailCheckResponse> checkEmail(
            @Valid @RequestBody EmailCheckRequest request
    ) {
        boolean available = authService.isEmailAvailable(request.getEmail());
        EmailCheckResponse response = available
                ? EmailCheckResponse.available()
                : EmailCheckResponse.duplicated();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 이름, 전화번호, 약관 동의로 회원가입")
    public ResponseEntity<SignUpResponse> signUp(
            @Valid @RequestBody SignUpRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        SignUpResponse response = authService.signUp(request, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일, 비밀번호로 로그인하여 Access Token + Refresh Token 발급")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        String deviceInfo = httpRequest.getHeader("User-Agent");

        // Rate Limiting 체크
        if (!rateLimitService.isLoginAllowed(ipAddress, MAX_LOGIN_ATTEMPTS, LOGIN_WINDOW_MINUTES)) {
            throw BusinessException.tooManyLoginAttempts(LOGIN_WINDOW_MINUTES * 60);
        }

        try {
            AuthService.LoginResult result = authService.login(request, ipAddress, deviceInfo);

            // 로그인 성공 시 Rate Limit 카운터 리셋
            rateLimitService.resetLoginAttempts(ipAddress);

            return ResponseEntity.ok(LoginResponse.of(
                    result.accessToken(),
                    result.refreshToken(),
                    result.expiresIn()
            ));
        } catch (BusinessException e) {
            // 로그인 실패 시 시도 횟수 기록
            rateLimitService.recordLoginAttempt(ipAddress, LOGIN_WINDOW_MINUTES);
            throw e;
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access Token 발급")
    public ResponseEntity<TokenRefreshResponse> refresh(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        String refreshToken = request.getRefreshToken();

        // Rate Limiting 체크 (토큰 해시값 기준)
        String tokenHash = String.valueOf(refreshToken.hashCode());
        if (!rateLimitService.isRefreshAllowed(tokenHash, MAX_REFRESH_ATTEMPTS, REFRESH_WINDOW_MINUTES)) {
            throw BusinessException.tooManyRefreshAttempts();
        }
        rateLimitService.recordRefreshAttempt(tokenHash, REFRESH_WINDOW_MINUTES);

        TokenRefreshResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Access Token 블랙리스트 등록 + Refresh Token 무효화")
    public ResponseEntity<Map<String, Boolean>> logout(
            @RequestBody(required = false) TokenRefreshRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        // Refresh Token (Body에서)
        String refreshToken = (request != null) ? request.getRefreshToken() : null;

        // Access Token 추출 (Bearer 제거)
        String accessToken = extractAccessToken(authorizationHeader);

        // 로그아웃 처리 (Access Token 블랙리스트 + Refresh Token 무효화)
        authService.logout(accessToken, refreshToken);

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "전체 로그아웃", description = "모든 기기에서 로그아웃 (현재 Access Token 블랙리스트 + 모든 Refresh Token 무효화)")
    public ResponseEntity<Map<String, Boolean>> logoutAll(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        // Access Token 추출 (Bearer 제거)
        String accessToken = extractAccessToken(authorizationHeader);

        authService.logoutAll(userId, accessToken);

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/change-password")
    @Operation(summary = "비밀번호 변경", description = "비밀번호 변경 후 새 토큰 발급. 다른 기기에서 로그아웃 옵션 제공")
    public ResponseEntity<LoginResponse> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader(value = "Authorization") String authorizationHeader,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        String deviceInfo = httpRequest.getHeader("User-Agent");

        // Access Token 추출 (Bearer 제거)
        String accessToken = extractAccessToken(authorizationHeader);

        AuthService.ChangePasswordResult result = authService.changePassword(
                userId,
                request.getCurrentPassword(),
                request.getNewPassword(),
                accessToken,
                request.isLogoutOtherDevices(),
                ipAddress,
                deviceInfo
        );

        return ResponseEntity.ok(LoginResponse.of(
                result.newAccessToken(),
                result.newRefreshToken(),
                result.expiresIn()
        ));
    }

    @GetMapping("/me")
    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필 및 알림 설정 조회")
    public ResponseEntity<ProfileResponse> getProfile(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(authService.getProfile(userId));
    }

    @PatchMapping("/profile")
    @Operation(summary = "프로필 수정", description = "이름, 전화번호, 프로필 이미지 URL 수정")
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PatchMapping("/notification-settings")
    @Operation(summary = "알림 설정 수정", description = "이메일/푸시/마케팅 알림 설정 저장")
    public ResponseEntity<ProfileResponse> updateNotificationSettings(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NotificationSettingsUpdateRequest request
    ) {
        return ResponseEntity.ok(authService.updateNotificationSettings(userId, request));
    }

    /**
     * Authorization 헤더에서 Access Token 추출
     */
    private String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    /**
     * 클라이언트 IP 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
