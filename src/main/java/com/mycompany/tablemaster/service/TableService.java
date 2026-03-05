package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.dto.table.TableListResponse;
import com.mycompany.tablemaster.dto.table.TableSetupRequest;
import com.mycompany.tablemaster.dto.table.TableSetupResponse;
import com.mycompany.tablemaster.dto.table.TableUpdateRequest;
import com.mycompany.tablemaster.entity.TableEntity;
import com.mycompany.tablemaster.entity.TableHistory;
import com.mycompany.tablemaster.entity.TableStatus;
import com.mycompany.tablemaster.event.TableEvent;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.messaging.producer.TableEventProducer;
import com.mycompany.tablemaster.repository.TableHistoryRepository;
import com.mycompany.tablemaster.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TableService {

    private final TableRepository tableRepository;
    private final TableHistoryRepository tableHistoryRepository;
    private final TableEventProducer tableEventProducer;
    private final WebSocketSenderService webSocketSenderService;

    /**
     * 모든 테이블 조회 (AVAILABLE, DELETED 제외 / INACTIVE 포함)
     */
    public List<TableListResponse> getAllTables() {
        return tableRepository.findByStatusNotIn(List.of(TableStatus.AVAILABLE, TableStatus.DELETED)).stream()
                .map(TableListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 테이블 상세 조회
     */
    public TableSetupResponse getTable(String deviceId) {
        TableEntity table = tableRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(
                        "테이블을 찾을 수 없습니다: " + deviceId,
                        HttpStatus.NOT_FOUND,
                        "TABLE_001"
                ));
        return TableSetupResponse.from(table);
    }

    /**
     * 디바이스 ID로 테이블 조회 (id = deviceId)
     */
    public TableSetupResponse getTableByDeviceId(String deviceId) {
        TableEntity table = tableRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(
                        "디바이스에 연결된 테이블이 없습니다",
                        HttpStatus.NOT_FOUND,
                        "TABLE_002"
                ));
        return TableSetupResponse.from(table);
    }

    /**
     * 테이블명으로 OCCUPIED 상태인 테이블 조회 (채팅 요청용)
     */
    public TableEntity getOccupiedTableByName(String name) {
        return tableRepository.findByNameAndStatus(name, TableStatus.OCCUPIED)
                .orElseThrow(() -> new BusinessException(
                        "해당 이름의 활성 테이블이 없습니다: " + name,
                        HttpStatus.NOT_FOUND,
                        "TABLE_005"
                ));
    }

    /**
     * 테이블 설정 (입장 시)
     * id(PK) = deviceId, name = tableId(앱에서 지정한 테이블명)
     */
    @Transactional
    public TableSetupResponse setupTable(TableSetupRequest request, String deviceId) {
        // 인원 수 검증
        if (request.getFemaleCount() + request.getMaleCount() != request.getGuestCount()) {
            throw new BusinessException(
                    "여성 인원과 남성 인원의 합이 총 인원과 일치하지 않습니다",
                    HttpStatus.BAD_REQUEST,
                    "TABLE_003"
            );
        }

        // 기존 테이블 확인 또는 새로 생성 (id = deviceId)
        TableEntity table = tableRepository.findById(deviceId)
                .orElseGet(() -> TableEntity.builder()
                        .id(deviceId)
                        .name(request.getTableId())
                        .build());

        // 테이블 설정 (name = tableId)
        table.setup(
                request.getTableId(),
                request.getLocation(),
                request.getGuestCount(),
                request.getFemaleCount(),
                request.getMaleCount()
        );

        TableEntity savedTable = tableRepository.save(table);
        log.info("Table setup completed: id={}, name={}", savedTable.getId(), savedTable.getName());

        // 테이블 추가 브로드캐스트
        broadcastTableAdded(savedTable);

        return TableSetupResponse.from(savedTable);
    }

    /**
     * 테이블 삭제 (히스토리로 이동 후 삭제)
     */
    @Transactional
    public void deleteTable(String deviceId) {
        TableEntity table = tableRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(
                        "테이블을 찾을 수 없습니다: " + deviceId,
                        HttpStatus.NOT_FOUND,
                        "TABLE_001"
                ));

        // 히스토리에 저장
        tableHistoryRepository.save(TableHistory.from(table));

        // 테이블 삭제
        tableRepository.delete(table);

        log.info("Table deleted and moved to history: deviceId={}", deviceId);

        // 삭제 이벤트 발행
        tableEventProducer.sendTableDeleted(TableEvent.deleted(deviceId, deviceId));

        // 테이블 삭제 브로드캐스트
        broadcastTableRemoved(deviceId);
    }

    /**
     * 테이블 수정
     */
    @Transactional
    public TableSetupResponse updateTable(String deviceId, TableUpdateRequest request) {
        TableEntity table = tableRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(
                        "테이블을 찾을 수 없습니다: " + deviceId,
                        HttpStatus.NOT_FOUND,
                        "TABLE_001"
                ));

        // 인원 정보 수정 (모두 제공된 경우에만 검증)
        if (request.getGuestCount() != null && request.getFemaleCount() != null && request.getMaleCount() != null) {
            if (request.getFemaleCount() + request.getMaleCount() != request.getGuestCount()) {
                throw new BusinessException(
                        "여성 인원과 남성 인원의 합이 총 인원과 일치하지 않습니다",
                        HttpStatus.BAD_REQUEST,
                        "TABLE_003"
                );
            }
            table.setGuestCount(request.getGuestCount());
            table.setFemaleCount(request.getFemaleCount());
            table.setMaleCount(request.getMaleCount());
        }

        // 위치 수정
        if (request.getLocation() != null) {
            table.setLocation(request.getLocation());
        }

        // 채팅 상태 수정
        if (request.getIsChatting() != null) {
            table.setIsChatting(request.getIsChatting());
        }

        TableEntity savedTable = tableRepository.save(table);
        log.info("Table updated: id={}", savedTable.getId());

        // 테이블 수정 브로드캐스트
        broadcastTableUpdated(savedTable);

        return TableSetupResponse.from(savedTable);
    }

    /**
     * 재연결 시 테이블 상태 웹 브로드캐스트 (deviceId로 조회 후 전송, DELETED 제외)
     */
    public void broadcastTableAdded(String deviceId) {
        tableRepository.findById(deviceId)
                .filter(t -> t.getStatus() != TableStatus.DELETED)
                .ifPresent(this::broadcastTableAdded);
    }

    /**
     * 디바이스/테이블 영구 삭제 처리 (DELETED 상태로 변경 후 대시보드에서 제거)
     */
    @Transactional
    public void markTableDeleted(String deviceId) {
        tableRepository.findById(deviceId).ifPresent(table -> {
            if (table.getStatus() != TableStatus.DELETED) {
                table.markDeleted();
                tableRepository.save(table);
                log.info("Table marked as deleted: deviceId={}", deviceId);
                broadcastTableRemoved(deviceId);
            }
        });
    }

    /**
     * 단일 테이블 추가 브로드캐스트
     */
    public void broadcastTableAdded(TableEntity table) {
        Map<String, Object> payload = Map.of(
                "type", "TABLE_ADDED",
                "data", TableListResponse.from(table),
                "timestamp", LocalDateTime.now().toString()
        );
        webSocketSenderService.broadcast("tables", payload);
        log.debug("Table added broadcasted: id={}", table.getId());
    }

    /**
     * 단일 테이블 수정 브로드캐스트
     */
    public void broadcastTableUpdated(TableEntity table) {
        Map<String, Object> payload = Map.of(
                "type", "TABLE_UPDATED",
                "data", TableListResponse.from(table),
                "timestamp", LocalDateTime.now().toString()
        );
        webSocketSenderService.broadcast("tables", payload);
        log.debug("Table updated broadcasted: id={}", table.getId());
    }

    /**
     * 단일 테이블 삭제 브로드캐스트
     */
    public void broadcastTableRemoved(String deviceId) {
        Map<String, Object> payload = Map.of(
                "type", "TABLE_REMOVED",
                "id", deviceId,
                "timestamp", LocalDateTime.now().toString()
        );
        webSocketSenderService.broadcast("tables", payload);
        log.debug("Table removed broadcasted: id={}", deviceId);
    }

    /**
     * 디바이스 연결 해제 시 테이블 비활성화
     */
    @Transactional
    public void deactivateTable(String deviceId) {
        tableRepository.findById(deviceId).ifPresent(table -> {
            if (table.isActive()) {
                table.deactivate();
                TableEntity savedTable = tableRepository.save(table);
                log.info("Table deactivated due to device disconnect: deviceId={}", deviceId);

                broadcastTableUpdated(savedTable);
            }
        });
    }

    /**
     * INACTIVE 테이블 삭제 (TTL 만료 시 호출)
     * 히스토리 저장 후 DB 삭제 + TABLE_REMOVED 브로드캐스트
     */
    @Transactional
    public void removeInactiveTable(String deviceId) {
        tableRepository.findById(deviceId).ifPresent(table -> {
            if (!table.isActive()) {
                tableHistoryRepository.save(TableHistory.from(table));
                tableRepository.delete(table);
                log.info("Inactive table removed after TTL: deviceId={}", deviceId);

                broadcastTableRemoved(deviceId);
            }
        });
    }

    /**
     * 디바이스 재연결 시 테이블 활성화
     */
    @Transactional
    public void activateTable(String deviceId) {
        tableRepository.findById(deviceId).ifPresent(table -> {
            if (!table.isActive()) {
                table.activate();
                TableEntity savedTable = tableRepository.save(table);
                log.info("Table activated due to device reconnect: deviceId={}", deviceId);

                if (savedTable.getStatus() == TableStatus.AVAILABLE) {
                    // AVAILABLE로 복원된 경우 대시보드에 노출하지 않음
                    return;
                }
                // OCCUPIED/CHATTING으로 복원 → 웹 입장에서 새로 등장하는 테이블이므로 TABLE_ADDED
                broadcastTableAdded(savedTable);
            }
        });
    }
}
