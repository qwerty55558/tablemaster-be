package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.config.properties.AuthValidationProperties;
import com.mycompany.tablemaster.dto.config.AuthConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
@Tag(name = "Config", description = "설정 API")
public class ConfigController {

    private final AuthValidationProperties authValidationProperties;

    @GetMapping("/auth")
    @Operation(summary = "인증 설정 조회", description = "회원가입 폼 유효성 검사 설정 조회")
    public ResponseEntity<AuthConfigResponse> getAuthConfig() {
        return ResponseEntity.ok(AuthConfigResponse.from(authValidationProperties));
    }
}
