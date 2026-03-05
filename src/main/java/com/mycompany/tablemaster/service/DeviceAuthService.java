package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.config.properties.DeviceProperties;
import com.mycompany.tablemaster.dto.device.DeviceLoginRequest;
import com.mycompany.tablemaster.dto.device.DeviceLoginResponse;
import com.mycompany.tablemaster.dto.device.DevicePendingRequest;
import com.mycompany.tablemaster.dto.device.DevicePendingResponse;
import com.mycompany.tablemaster.dto.device.DeviceRegisterRequest;
import com.mycompany.tablemaster.dto.device.DeviceResponse;
import com.mycompany.tablemaster.dto.device.DeviceUpdateRequest;
import com.mycompany.tablemaster.entity.DeviceWhitelist;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.repository.DeviceWhitelistRepository;
import com.mycompany.tablemaster.security.JwtTokenProvider;
import com.mycompany.tablemaster.websocket.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceAuthService {

    private final DeviceWhitelistRepository deviceWhitelistRepository;
    private final DeviceProperties deviceProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final WebSocketSenderService webSocketSenderService;
    private final WebSocketSessionRegistry sessionRegistry;
    private final TableService tableService;

    private static final String PENDING_KEY_PREFIX = "device:pending:";
    private static final long PENDING_TTL_SECONDS = 180; // 3분

    @Transactional
    public DeviceLoginResponse login(DeviceLoginRequest request) {
        // 1. App Secret 검증
        if (!deviceProperties.getAppSecret().equals(request.getAppSecret())) {
            log.warn("Invalid app secret for device: {}", request.getDeviceId());
            throw BusinessException.invalidAppSecret();
        }

        // 2. Device ID 화이트리스트 확인
        DeviceWhitelist device = deviceWhitelistRepository
                .findByDeviceIdAndIsActiveTrue(request.getDeviceId())
                .orElseThrow(() -> {
                    log.warn("Device not found or inactive: {}", request.getDeviceId());
                    return BusinessException.deviceNotFound();
                });

        // 3. 마지막 로그인 시간 업데이트
        device.setLastLoginAt(LocalDateTime.now());

        // 4. 토큰 발급
        String accessToken = jwtTokenProvider.createDeviceAccessToken(
                device.getId(),
                device.getDeviceId(),
                device.getDeviceName()
        );
        String refreshToken = jwtTokenProvider.createDeviceRefreshToken(
                device.getId(),
                device.getDeviceId()
        );

        log.info("Device login successful: {} ({})", device.getDeviceName(), device.getDeviceId());

        return DeviceLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationInSeconds())
                .tokenType("Bearer")
                .deviceName(device.getDeviceName())
                .build();
    }

    // ============== Admin 기능 ==============

    @Transactional(readOnly = true)
    public List<DeviceResponse> getAllDevices() {
        return deviceWhitelistRepository.findAll().stream()
                .map(DeviceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDevice(String deviceId) {
        DeviceWhitelist device = deviceWhitelistRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException("디바이스를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "DEVICE_004"));
        return DeviceResponse.from(device);
    }

    @Transactional
    public DeviceResponse registerDevice(DeviceRegisterRequest request) {
        // 중복 체크
        if (deviceWhitelistRepository.existsByDeviceId(request.getDeviceId())) {
            throw new BusinessException("이미 등록된 디바이스입니다", HttpStatus.CONFLICT, "DEVICE_005");
        }

        DeviceWhitelist device = DeviceWhitelist.builder()
                .deviceId(request.getDeviceId())
                .deviceName(request.getDeviceName())
                .isActive(true)
                .build();

        DeviceWhitelist saved = deviceWhitelistRepository.save(device);
        log.info("Device registered: {} ({})", saved.getDeviceName(), saved.getDeviceId());

        return DeviceResponse.from(saved);
    }

    @Transactional
    public DeviceResponse updateDevice(String deviceId, DeviceUpdateRequest request) {
        DeviceWhitelist device = deviceWhitelistRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException("디바이스를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "DEVICE_004"));

        device.setDeviceName(request.getDeviceName());
        log.info("Device updated: {} ({})", device.getDeviceName(), device.getDeviceId());

        return DeviceResponse.from(device);
    }

    @Transactional
    public DeviceResponse toggleDeviceActive(String deviceId) {
        DeviceWhitelist device = deviceWhitelistRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException("디바이스를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "DEVICE_004"));

        device.setIsActive(!device.getIsActive());
        log.info("Device {} {}", device.getDeviceId(), device.getIsActive() ? "activated" : "deactivated");

        return DeviceResponse.from(device);
    }

    @Transactional
    public void deleteDevice(String deviceId) {
        DeviceWhitelist device = deviceWhitelistRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException("디바이스를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "DEVICE_004"));

        // 1. 테이블 DELETED 처리 (이력 보존, 대시보드에서 제거)
        tableService.markTableDeleted(deviceId);

        // 2. 디바이스에 삭제 메시지 전송 (연결된 상태에서)
        webSocketSenderService.sendToDevice(deviceId, Map.of(
                "type", "DEVICE_DELETED",
                "deviceId", deviceId,
                "timestamp", java.time.Instant.now().toString()
        ));

        // 3. whitelist DB 삭제
        deviceWhitelistRepository.delete(device);
        log.info("Device deleted: {} ({})", device.getDeviceName(), device.getDeviceId());
    }

    // ============== 등록 요청 (Redis) ==============

    /**
     * 디바이스 등록 요청 (Redis에 저장, TTL 3분)
     */
    public Map<String, String> requestRegister(DevicePendingRequest request) {
        // App Secret 검증
        if (!deviceProperties.getAppSecret().equals(request.getAppSecret())) {
            log.warn("Invalid app secret for device registration: {}", request.getDeviceId());
            throw BusinessException.invalidAppSecret();
        }

        String key = PENDING_KEY_PREFIX + request.getDeviceId();
        String requestedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // 이미 등록된 디바이스인지 확인
        if (deviceWhitelistRepository.existsByDeviceId(request.getDeviceId())) {
            throw new BusinessException("이미 등록된 디바이스입니다", HttpStatus.CONFLICT, "DEVICE_005");
        }

        // Redis에 저장 (Hash 사용)
        redisTemplate.opsForHash().put(key, "requestedAt", requestedAt);
        redisTemplate.expire(key, PENDING_TTL_SECONDS, TimeUnit.SECONDS);

        log.info("Device registration requested: {}", request.getDeviceId());

        // 관리자에게 실시간 알림 전송
        webSocketSenderService.sendToAdmins(Map.of(
                "type", "DEVICE_REGISTRATION_REQUEST",
                "deviceId", request.getDeviceId(),
                "requestedAt", requestedAt,
                "ttl", PENDING_TTL_SECONDS
        ));

        return Map.of(
                "status", "pending",
                "message", "등록 요청이 접수되었습니다. 관리자 승인을 기다려주세요.",
                "ttl", String.valueOf(PENDING_TTL_SECONDS)
        );
    }

    /**
     * 디바이스 등록 상태 확인
     */
    public Map<String, String> getDeviceStatus(String deviceId) {
        // 1. DB에 있으면 승인됨
        if (deviceWhitelistRepository.existsByDeviceId(deviceId)) {
            return Map.of("status", "approved", "message", "승인된 디바이스입니다.");
        }

        // 2. Redis에 있으면 대기 중
        String key = PENDING_KEY_PREFIX + deviceId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return Map.of(
                    "status", "pending",
                    "message", "승인 대기 중입니다.",
                    "ttl", String.valueOf(ttl != null ? ttl : 0)
            );
        }

        // 3. 둘 다 없으면 미등록
        return Map.of("status", "not_found", "message", "등록되지 않은 디바이스입니다.");
    }

    /**
     * 대기 중인 디바이스 목록 조회 (Admin)
     */
    public List<DevicePendingResponse> getPendingDevices() {
        Set<String> keys = redisTemplate.keys(PENDING_KEY_PREFIX + "*");
        List<DevicePendingResponse> pendingList = new ArrayList<>();

        if (keys != null) {
            for (String key : keys) {
                String deviceId = key.replace(PENDING_KEY_PREFIX, "");
                Object requestedAtObj = redisTemplate.opsForHash().get(key, "requestedAt");
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

                pendingList.add(DevicePendingResponse.builder()
                        .deviceId(deviceId)
                        .requestedAt(requestedAtObj != null ? requestedAtObj.toString() : "")
                        .ttl(ttl != null ? ttl : 0)
                        .build());
            }
        }

        return pendingList;
    }

    /**
     * 디바이스 등록 승인 (Redis → DB)
     */
    @Transactional
    public DeviceResponse approveDevice(String deviceId, String deviceName) {
        String key = PENDING_KEY_PREFIX + deviceId;

        // Redis에서 정보 조회
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            throw new BusinessException("등록 요청을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "DEVICE_006");
        }

        // DB에 저장
        DeviceWhitelist device = DeviceWhitelist.builder()
                .deviceId(deviceId)
                .deviceName(deviceName)
                .isActive(true)
                .build();

        DeviceWhitelist saved = deviceWhitelistRepository.save(device);

        // Redis에서 삭제
        redisTemplate.delete(key);

        log.info("Device approved: {} ({})", saved.getDeviceName(), saved.getDeviceId());

        return DeviceResponse.from(saved);
    }
}
