package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.dto.notification.NotificationDTO;
import com.mycompany.tablemaster.dto.sync.SyncResponse;
import com.mycompany.tablemaster.dto.table.TableListResponse;
import com.mycompany.tablemaster.dto.table.TableSetupResponse;
import com.mycompany.tablemaster.entity.Notification;
import com.mycompany.tablemaster.entity.TableEntity;
import com.mycompany.tablemaster.repository.NotificationRepository;
import com.mycompany.tablemaster.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SyncService {

    private final TableRepository tableRepository;
    private final NotificationRepository notificationRepository;
    private final TableService tableService;

    /**
     * 디바이스 동기화 데이터 조회
     */
    public SyncResponse getSyncData(String deviceId) {
        log.info("Getting sync data for device: {}", deviceId);

        // 1. 전체 테이블 목록 조회 (AVAILABLE 제외, INACTIVE 포함)
        List<TableListResponse> tables = tableService.getAllTables();

        // 2. 내 테이블 상태 조회 (id = deviceId)
        TableEntity myTable = tableRepository.findById(deviceId).orElse(null);

        // 3. 미전달 알림 조회
        List<Notification> notifications = notificationRepository
                .findByDeviceIdAndIsDeliveredFalseOrderByCreatedAtAsc(deviceId);

        log.info("Sync data retrieved: deviceId={}, tableCount={}, hasMyTable={}, notificationCount={}",
                deviceId, tables.size(), myTable != null, notifications.size());

        return SyncResponse.builder()
                .tables(tables)
                .table(myTable != null ? TableSetupResponse.from(myTable) : null)
                .notifications(notifications.stream()
                        .map(NotificationDTO::from)
                        .toList())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
