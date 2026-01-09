package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.notification.NotificationDTO;
import com.mycompany.tablemaster.security.JwtTokenProvider;
import com.mycompany.tablemaster.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/device/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "디바이스 알림 API")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    @Operation(summary = "알림 목록 조회")
    public ResponseEntity<Page<NotificationDTO>> getNotifications(
            @RequestHeader("Authorization") String authHeader,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        String deviceId = extractDeviceId(authHeader);
        return ResponseEntity.ok(notificationService.getNotifications(deviceId, pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 개수")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @RequestHeader("Authorization") String authHeader) {

        String deviceId = extractDeviceId(authHeader);
        long count = notificationService.getUnreadCount(deviceId);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "알림 읽음 처리")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        String deviceId = extractDeviceId(authHeader);
        notificationService.markAsRead(id, deviceId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    @Operation(summary = "모든 알림 읽음 처리")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader("Authorization") String authHeader) {

        String deviceId = extractDeviceId(authHeader);
        notificationService.markAllAsRead(deviceId);
        return ResponseEntity.ok().build();
    }

    private String extractDeviceId(String authHeader) {
        String token = authHeader.substring(7);
        return jwtTokenProvider.getDeviceId(token);
    }

    public record UnreadCountResponse(long unreadCount) {}
}
