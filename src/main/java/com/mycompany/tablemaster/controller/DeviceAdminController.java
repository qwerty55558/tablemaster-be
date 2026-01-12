package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.device.DeviceApproveRequest;
import com.mycompany.tablemaster.dto.device.DevicePendingResponse;
import com.mycompany.tablemaster.dto.device.DeviceRegisterRequest;
import com.mycompany.tablemaster.dto.device.DeviceResponse;
import com.mycompany.tablemaster.dto.device.DeviceUpdateRequest;
import com.mycompany.tablemaster.service.DeviceAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mycompany.tablemaster.config.properties.DeviceProperties;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/devices")
@RequiredArgsConstructor
@Tag(name = "Device Admin", description = "디바이스 관리 API (관리자 전용)")
public class DeviceAdminController {

    private final DeviceAuthService deviceAuthService;
    private final DeviceProperties deviceProperties;

    @GetMapping
    @Operation(summary = "디바이스 목록 조회", description = "등록된 모든 디바이스 목록 조회")
    public ResponseEntity<List<DeviceResponse>> getAllDevices() {
        return ResponseEntity.ok(deviceAuthService.getAllDevices());
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "디바이스 상세 조회", description = "특정 디바이스 정보 조회")
    public ResponseEntity<DeviceResponse> getDevice(@PathVariable String deviceId) {
        return ResponseEntity.ok(deviceAuthService.getDevice(deviceId));
    }

    @PostMapping
    @Operation(summary = "디바이스 등록", description = "새 디바이스를 화이트리스트에 등록")
    public ResponseEntity<DeviceResponse> registerDevice(@Valid @RequestBody DeviceRegisterRequest request) {
        return ResponseEntity.ok(deviceAuthService.registerDevice(request));
    }

    @PatchMapping("/{deviceId}")
    @Operation(summary = "디바이스 수정", description = "디바이스 이름 수정")
    public ResponseEntity<DeviceResponse> updateDevice(
            @PathVariable String deviceId,
            @Valid @RequestBody DeviceUpdateRequest request) {
        return ResponseEntity.ok(deviceAuthService.updateDevice(deviceId, request));
    }

    @PatchMapping("/{deviceId}/toggle")
    @Operation(summary = "디바이스 활성화/비활성화", description = "디바이스 활성화 상태 토글")
    public ResponseEntity<DeviceResponse> toggleDeviceActive(@PathVariable String deviceId) {
        return ResponseEntity.ok(deviceAuthService.toggleDeviceActive(deviceId));
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "디바이스 삭제", description = "디바이스를 화이트리스트에서 삭제")
    public ResponseEntity<Map<String, Boolean>> deleteDevice(@PathVariable String deviceId) {
        deviceAuthService.deleteDevice(deviceId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @GetMapping("/secret")
    @Operation(summary = "App Secret 조회", description = "디바이스 인증용 App Secret 조회 (관리자 전용)")
    public ResponseEntity<Map<String, String>> getAppSecret() {
        return ResponseEntity.ok(Map.of("appSecret", deviceProperties.getAppSecret()));
    }

    @GetMapping("/pending")
    @Operation(summary = "대기 중인 디바이스 목록", description = "등록 대기 중인 디바이스 목록 조회 (TTL 3분)")
    public ResponseEntity<List<DevicePendingResponse>> getPendingDevices() {
        return ResponseEntity.ok(deviceAuthService.getPendingDevices());
    }

    @PostMapping("/approve/{deviceId}")
    @Operation(summary = "디바이스 등록 승인", description = "대기 중인 디바이스 등록 승인")
    public ResponseEntity<DeviceResponse> approveDevice(
            @PathVariable String deviceId,
            @Valid @RequestBody DeviceApproveRequest request) {
        return ResponseEntity.ok(deviceAuthService.approveDevice(deviceId, request.getDeviceName()));
    }
}
