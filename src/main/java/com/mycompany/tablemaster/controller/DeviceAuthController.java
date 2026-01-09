package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.device.DeviceLoginRequest;
import com.mycompany.tablemaster.dto.device.DeviceLoginResponse;
import com.mycompany.tablemaster.dto.device.DevicePendingRequest;
import com.mycompany.tablemaster.service.DeviceAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/device")
@RequiredArgsConstructor
@Tag(name = "Device Auth", description = "디바이스 인증 API")
public class DeviceAuthController {

    private final DeviceAuthService deviceAuthService;

    @PostMapping("/login")
    @Operation(summary = "디바이스 로그인", description = "Device ID + App Secret으로 로그인하여 토큰 발급")
    public ResponseEntity<DeviceLoginResponse> login(@Valid @RequestBody DeviceLoginRequest request) {
        DeviceLoginResponse response = deviceAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "디바이스 등록 요청", description = "디바이스 등록을 요청합니다. 관리자 승인 후 로그인 가능 (TTL 3분)")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody DevicePendingRequest request) {
        return ResponseEntity.ok(deviceAuthService.requestRegister(request));
    }

    @GetMapping("/status/{deviceId}")
    @Operation(summary = "디바이스 등록 상태 확인", description = "디바이스 등록 승인 여부 확인 (pending/approved/not_found)")
    public ResponseEntity<Map<String, String>> getStatus(@PathVariable String deviceId) {
        return ResponseEntity.ok(deviceAuthService.getDeviceStatus(deviceId));
    }
}
