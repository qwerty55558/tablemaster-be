package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.sync.SyncRequest;
import com.mycompany.tablemaster.dto.sync.SyncResponse;
import com.mycompany.tablemaster.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SyncController {

    private final SyncService syncService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트 동기화 요청 처리
     * 클라이언트: /app/sync 로 발행
     * 서버: /queue/tables, /queue/myTable, /queue/notifications 로 응답
     */
    @MessageMapping("/sync")
    public void handleSync(SyncRequest request, Principal principal) {
        String deviceId = principal.getName();
        log.info("Sync requested: deviceId={}", deviceId);

        SyncResponse response = syncService.getSyncData(deviceId);

        // 전체 테이블 목록 전송 → /queue/tables (싱크용)
        messagingTemplate.convertAndSendToUser(
                deviceId,
                "/queue/tables",
                Map.of(
                        "type", "TABLES_SNAPSHOT",
                        "data", response.getTables(),
                        "timestamp", LocalDateTime.now().toString()
                )
        );
        log.debug("Sync: {} tables sent to device {}", response.getTables().size(), deviceId);

        // 내 테이블 상태 전송 → /queue/myTable
        if (response.getTable() != null) {
            messagingTemplate.convertAndSendToUser(
                    deviceId,
                    "/queue/myTable",
                    Map.of("type", "TABLE_UPDATED", "data", response.getTable())
            );
            log.debug("Sync: myTable data sent to device {}", deviceId);
        }

        // 미전달 알림 전송 → /queue/notifications
        if (!response.getNotifications().isEmpty()) {
            messagingTemplate.convertAndSendToUser(
                    deviceId,
                    "/queue/notifications",
                    Map.of("type", "SYNC_NOTIFICATIONS", "data", response.getNotifications())
            );
            log.debug("Sync: {} notifications sent to device {}", response.getNotifications().size(), deviceId);
        }

        log.info("Sync completed: deviceId={}", deviceId);
    }
}
