package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.dto.table.AvailableDeviceResponse;
import com.mycompany.tablemaster.dto.table.TableHistoryListResponse;
import com.mycompany.tablemaster.dto.table.TableHistoryRequest;
import com.mycompany.tablemaster.dto.table.TableHistoryResponse;
import com.mycompany.tablemaster.dto.table.TableListResponse;
import com.mycompany.tablemaster.dto.table.TableSetupRequest;
import com.mycompany.tablemaster.dto.table.TableSetupResponse;
import com.mycompany.tablemaster.dto.table.TableUpdateRequest;
import com.mycompany.tablemaster.entity.DeviceWhitelist;
import com.mycompany.tablemaster.entity.BillStatus;
import com.mycompany.tablemaster.repository.DeviceWhitelistRepository;
import com.mycompany.tablemaster.entity.TableEntity;
import com.mycompany.tablemaster.entity.TableHistory;
import com.mycompany.tablemaster.entity.TableStatus;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.repository.BillRepository;
import com.mycompany.tablemaster.repository.TableHistoryRepository;
import com.mycompany.tablemaster.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
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
    private final DeviceWhitelistRepository deviceWhitelistRepository;
    private final BillRepository billRepository;
    private final WebSocketSenderService webSocketSenderService;
    private final ChatRoomService chatRoomService;
    private final AnalyticsLogService analyticsLogService;

    /**
     * 모든 테이블 조회 (AVAILABLE, DELETED 제외 / INACTIVE 포함)
     * 채팅 제재/음소거 상태 포함
     */
    public List<TableListResponse> getAllTables() {
        List<TableEntity> tables = tableRepository.findByStatusNotIn(List.of(TableStatus.AVAILABLE, TableStatus.DELETED));
        List<String> deviceIds = tables.stream().map(TableEntity::getId).toList();
        Map<String, ChatRoomService.ChatStatusInfo> chatStatusMap = chatRoomService.getChatStatusByDeviceIds(deviceIds);

        return tables.stream()
                .map(t -> {
                    ChatRoomService.ChatStatusInfo info = chatStatusMap.get(t.getId());
                    return info != null
                            ? TableListResponse.from(t, info.chatRoomId(), info.sanctionType(), info.isMuted(), info.sanctionExpiresAt())
                            : TableListResponse.from(t);
                })
                .collect(Collectors.toList());
    }

    /**
     * 최근 30일 테이블 입장 기록 조회 (offset 기반 페이지네이션)
     */
    public TableHistoryListResponse getTableHistory(TableHistoryRequest request) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int safeOffset = Math.max(request.getOffset(), 0);
        int safeLimit = Math.max(request.getLimit(), 1);

        List<TableHistoryResponse> histories = tableHistoryRepository.findByDeletedAtAfterOrderByDeletedAtDesc(thirtyDaysAgo)
                .stream()
                .map(TableHistoryResponse::from)
                .toList();

        List<TableHistoryResponse> activeTables = tableRepository
                .findByStatusNotIn(List.of(TableStatus.AVAILABLE, TableStatus.DELETED))
                .stream()
                .filter(table -> table.getCreatedAt() != null && !table.getCreatedAt().isBefore(thirtyDaysAgo))
                .map(TableHistoryResponse::from)
                .toList();

        List<TableHistoryResponse> merged = java.util.stream.Stream.concat(histories.stream(), activeTables.stream())
                .sorted(Comparator.comparing(TableHistoryResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int fromIndex = Math.min(safeOffset, merged.size());
        int toIndex = Math.min(fromIndex + safeLimit, merged.size());
        List<TableHistoryResponse> content = merged.subList(fromIndex, toIndex);

        return TableHistoryListResponse.builder()
                .content(content)
                .totalCount(merged.size())
                .offset(safeOffset)
                .limit(safeLimit)
                .hasNext(toIndex < merged.size())
                .build();
    }

    /**
     * 빈 디바이스 조회 (화이트리스트에 등록됨 + 테이블 매칭 안 됨)
     * 테이블이 없거나 AVAILABLE 상태인 디바이스 반환
     */
    public List<AvailableDeviceResponse> getAvailableDevices() {
        List<DeviceWhitelist> activeDevices = deviceWhitelistRepository.findAll().stream()
                .filter(DeviceWhitelist::getIsActive)
                .toList();

        List<String> occupiedDeviceIds = tableRepository.findByStatusNotIn(
                List.of(TableStatus.AVAILABLE, TableStatus.DELETED)
        ).stream().map(TableEntity::getId).toList();

        return activeDevices.stream()
                .filter(device -> !occupiedDeviceIds.contains(device.getDeviceId()))
                .map(AvailableDeviceResponse::from)
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

        // 화이트리스트에서 디바이스 이름 조회
        String deviceName = deviceWhitelistRepository.findByDeviceId(deviceId)
                .map(DeviceWhitelist::getDeviceName)
                .orElse(null);

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
        table.setDeviceName(deviceName);

        TableEntity savedTable = tableRepository.save(table);
        log.info("Table setup completed: id={}, name={}", savedTable.getId(), savedTable.getName());
        analyticsLogService.logVisitorEntry(savedTable);

        // 웹 브로드캐스트
        broadcastTableAdded(savedTable);

        return TableSetupResponse.from(savedTable);
    }

    /**
     * 테이블 삭제 (히스토리로 이동 후 삭제)
     */
    @Transactional
    public void deleteTable(String identifier) {
        TableEntity table = resolveTableByIdentifier(identifier);
        String deviceId = table.getId();

        validateNoOpenBill(deviceId);

        // 채팅 데이터 로그 후 삭제
        chatRoomService.cleanupByDeviceId(deviceId);
        analyticsLogService.logVisitorExit(table, "TABLE_DELETED");

        // 히스토리에 저장
        tableHistoryRepository.save(TableHistory.from(table));

        // 테이블 삭제
        tableRepository.delete(table);

        log.info("Table deleted and moved to history: deviceId={}", deviceId);

        // 브로드캐스트
        broadcastTableRemoved(deviceId);
    }

    private TableEntity resolveTableByIdentifier(String identifier) {
        return tableRepository.findById(identifier)
                .orElseGet(() -> {
                    List<TableEntity> matchedTables = tableRepository.findByName(identifier).stream()
                            .filter(table -> table.getStatus() != TableStatus.DELETED)
                            .toList();

                    if (matchedTables.isEmpty()) {
                        throw new BusinessException(
                                "테이블을 찾을 수 없습니다: " + identifier,
                                HttpStatus.NOT_FOUND,
                                "TABLE_001"
                        );
                    }

                    if (matchedTables.size() > 1) {
                        throw new BusinessException(
                                "동일한 테이블명이 여러 건 존재합니다. deviceId로 요청해주세요: " + identifier,
                                HttpStatus.BAD_REQUEST,
                                "TABLE_006"
                        );
                    }

                    return matchedTables.get(0);
                });
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

        // 채팅 허용 상태 수정
        if (request.getIsChatEnabled() != null) {
            table.setIsChatEnabled(request.getIsChatEnabled());
        }

        // 채팅 중 상태 수정
        if (request.getIsChatting() != null) {
            table.setIsChatting(request.getIsChatting());
        }

        TableEntity savedTable = tableRepository.save(table);
        log.info("Table updated: id={}", savedTable.getId());
        analyticsLogService.logVisitorUpdate(savedTable);

        // 웹 브로드캐스트
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
     * 디바이스 삭제 시 연동 테이블 처리 (히스토리 저장 후 DB 삭제)
     */
    @Transactional
    public void markTableDeleted(String deviceId) {
        tableRepository.findById(deviceId).ifPresent(table -> {
            if (table.getStatus() != TableStatus.DELETED) {
                validateNoOpenBill(deviceId);

                // 채팅 데이터 히스토리 저장 후 삭제
                chatRoomService.cleanupByDeviceId(deviceId);

                analyticsLogService.logVisitorExit(table, "DEVICE_DELETED");

                // AVAILABLE 상태가 아닌 경우(손님이 있는 경우)만 히스토리 저장
                if (table.getStatus() != TableStatus.AVAILABLE) {
                    tableHistoryRepository.save(TableHistory.from(table));
                }
                tableRepository.delete(table);
                log.info("Table deleted and moved to history: deviceId={}", deviceId);
                broadcastTableRemoved(deviceId);
            }
        });
    }

    /**
     * 단일 테이블 추가 브로드캐스트 (채팅 상태 포함)
     */
    public void broadcastTableAdded(TableEntity table) {
        Map<String, Object> payload = Map.of(
                "type", "TABLE_ADDED",
                "data", enrichWithChatStatus(table),
                "timestamp", LocalDateTime.now().toString()
        );
        webSocketSenderService.broadcast("tables", payload);
        log.debug("Table added broadcasted: id={}", table.getId());
    }

    /**
     * 단일 테이블 수정 브로드캐스트 (채팅 상태 포함)
     */
    public void broadcastTableUpdated(TableEntity table) {
        Map<String, Object> payload = Map.of(
                "type", "TABLE_UPDATED",
                "data", enrichWithChatStatus(table),
                "timestamp", LocalDateTime.now().toString()
        );
        webSocketSenderService.broadcast("tables", payload);
        log.debug("Table updated broadcasted: id={}", table.getId());
    }

    private TableListResponse enrichWithChatStatus(TableEntity table) {
        Map<String, ChatRoomService.ChatStatusInfo> statusMap =
                chatRoomService.getChatStatusByDeviceIds(List.of(table.getId()));
        ChatRoomService.ChatStatusInfo info = statusMap.get(table.getId());
        return info != null
                ? TableListResponse.from(table, info.chatRoomId(), info.sanctionType(), info.isMuted(), info.sanctionExpiresAt())
                : TableListResponse.from(table);
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
                chatRoomService.cleanupByDeviceId(deviceId);
                analyticsLogService.logVisitorExit(table, "INACTIVE_TTL_EXPIRED");
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
                analyticsLogService.logVisitorReconnect(savedTable);

                if (savedTable.getStatus() == TableStatus.AVAILABLE) {
                    // AVAILABLE로 복원된 경우 대시보드에 노출하지 않음
                    return;
                }
                // OCCUPIED/CHATTING으로 복원 → 대시보드에 INACTIVE로 이미 존재하므로 TABLE_UPDATED
                broadcastTableUpdated(savedTable);
            }
        });
    }

    private void validateNoOpenBill(String deviceId) {
        if (billRepository.findByDeviceIdAndStatus(deviceId, BillStatus.OPEN).isPresent()) {
            throw BusinessException.tableHasOpenBill();
        }
    }
}
