package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.auth.EmailCheckRequest;
import com.mycompany.tablemaster.dto.auth.EmailCheckResponse;
import com.mycompany.tablemaster.dto.auth.SignUpRequest;
import com.mycompany.tablemaster.dto.auth.SignUpResponse;
import com.mycompany.tablemaster.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

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

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
