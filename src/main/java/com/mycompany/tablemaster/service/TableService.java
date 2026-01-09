package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.dto.table.TableListResponse;
import com.mycompany.tablemaster.dto.table.TableSetupRequest;
import com.mycompany.tablemaster.dto.table.TableSetupResponse;
import com.mycompany.tablemaster.entity.TableEntity;
import com.mycompany.tablemaster.entity.TableStatus;
import com.mycompany.tablemaster.event.TableEvent;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.messaging.producer.TableEventProducer;
import com.mycompany.tablemaster.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TableService {

    private final TableRepository tableRepository;
    private final TableEventProducer tableEventProducer;
    private final WebSocketSenderService webSocketSenderService;

    /**
     * 모든 테이블 조회
     */
    public List<TableListResponse> getAllTables() {
        return tableRepository.findAll().stream()
                .map(TableListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 테이블 상세 조회
     */
    public TableSetupResponse getTable(String tableId) {
        TableEntity table = tableRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException(
                        "테이블을 찾을 수 없습니다: " + tableId,
                        HttpStatus.NOT_FOUND,
                        "TABLE_001"
                ));
        return TableSetupResponse.from(table);
    }

    /**
     * 디바이스 ID로 테이블 조회
     */
    public TableSetupResponse getTableByDeviceId(String deviceId) {
        TableEntity table = tableRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(
                        "디바이스에 연결된 테이블이 없습니다",
                        HttpStatus.NOT_FOUND,
                        "TABLE_002"
                ));
        return TableSetupResponse.from(table);
    }

    /**
     * 테이블 설정 (입장 시)
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

        // 기존 테이블 확인 또는 새로 생성
        TableEntity table = tableRepository.findById(request.getTableId())
                .orElseGet(() -> TableEntity.builder()
                        .id(request.getTableId())
                        .name(request.getTableId())
                        .build());

        // 이미 사용 중인 테이블인지 확인
        if (table.getStatus() == TableStatus.OCCUPIED &&
            table.getDeviceId() != null &&
            !table.getDeviceId().equals(deviceId)) {
            throw new BusinessException(
                    "이미 사용 중인 테이블입니다",
                    HttpStatus.CONFLICT,
                    "TABLE_004"
            );
        }

        // 테이블 설정
        table.setup(
                request.getLocation(),
                request.getGuestCount(),
                request.getFemaleCount(),
                request.getMaleCount(),
                deviceId
        );

        TableEntity savedTable = tableRepository.save(table);
        log.info("Table setup completed: tableId={}, deviceId={}", savedTable.getId(), deviceId);

        // 테이블 업데이트 브로드캐스트
        broadcastTableUpdate();

        return TableSetupResponse.from(savedTable);
    }

    /**
     * 테이블 초기화 (관리자용)
     */
    @Transactional
    public void resetTable(String tableId) {
        TableEntity table = tableRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException(
                        "테이블을 찾을 수 없습니다: " + tableId,
                        HttpStatus.NOT_FOUND,
                        "TABLE_001"
                ));

        String deviceId = table.getDeviceId();

        // 테이블 초기화
        table.reset();
        tableRepository.save(table);
        log.info("Table reset completed: tableId={}", tableId);

        // RabbitMQ로 초기화 이벤트 발행
        tableEventProducer.sendTableReset(TableEvent.reset(tableId, deviceId));

        // 테이블 업데이트 브로드캐스트
        broadcastTableUpdate();
    }

    /**
     * 테이블 목록 업데이트 브로드캐스트
     */
    public void broadcastTableUpdate() {
        List<TableListResponse> tables = getAllTables();
        webSocketSenderService.broadcast("tables", tables);
        log.debug("Table update broadcasted: {} tables", tables.size());
    }
}
