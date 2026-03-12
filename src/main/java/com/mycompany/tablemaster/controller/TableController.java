package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.table.AvailableDeviceResponse;
import com.mycompany.tablemaster.dto.table.TableHistoryListResponse;
import com.mycompany.tablemaster.dto.table.TableHistoryRequest;
import com.mycompany.tablemaster.dto.table.TableListResponse;
import com.mycompany.tablemaster.dto.table.TableSetupRequest;
import com.mycompany.tablemaster.dto.table.TableSetupResponse;
import com.mycompany.tablemaster.dto.table.TableUpdateRequest;
import com.mycompany.tablemaster.service.TableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
@Tag(name = "Table", description = "테이블 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class TableController {

    private final TableService tableService;

    @GetMapping
    @Operation(summary = "테이블 목록 조회", description = "모든 테이블 목록을 조회합니다")
    public ResponseEntity<List<TableListResponse>> getAllTables() {
        return ResponseEntity.ok(tableService.getAllTables());
    }

    @GetMapping("/available")
    @Operation(summary = "빈 디바이스 조회", description = "화이트리스트에 등록된 디바이스 중 테이블 미등록 디바이스 목록")
    public ResponseEntity<List<AvailableDeviceResponse>> getAvailableDevices() {
        return ResponseEntity.ok(tableService.getAvailableDevices());
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "테이블 상세 조회", description = "특정 테이블의 상세 정보를 조회합니다")
    public ResponseEntity<TableSetupResponse> getTable(@PathVariable String deviceId) {
        return ResponseEntity.ok(tableService.getTable(deviceId));
    }

    @GetMapping("/my")
    @Operation(summary = "내 테이블 조회", description = "현재 디바이스에 연결된 테이블 정보를 조회합니다")
    public ResponseEntity<TableSetupResponse> getMyTable(Principal principal) {
        String deviceId = principal.getName();
        return ResponseEntity.ok(tableService.getTableByDeviceId(deviceId));
    }

    @PostMapping("/setup")
    @Operation(summary = "테이블 설정 (디바이스)", description = "디바이스에서 직접 입장 등록")
    public ResponseEntity<TableSetupResponse> setupTable(
            @Valid @RequestBody TableSetupRequest request,
            Principal principal
    ) {
        String deviceId = principal.getName();
        return ResponseEntity.ok(tableService.setupTable(request, deviceId));
    }

    @PostMapping("/setup/{deviceId}")
    @Operation(summary = "테이블 설정 (프론트)", description = "프론트에서 특정 디바이스에 테이블 입장 등록")
    public ResponseEntity<TableSetupResponse> setupTableForDevice(
            @PathVariable String deviceId,
            @Valid @RequestBody TableSetupRequest request
    ) {
        return ResponseEntity.ok(tableService.setupTable(request, deviceId));
    }

    @PatchMapping("/{deviceId}")
    @Operation(summary = "테이블 수정", description = "테이블 정보를 수정합니다 (인원, 위치, 채팅 상태)")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('DEVICE') and principal.name == #deviceId)")
    public ResponseEntity<TableSetupResponse> updateTable(
            @PathVariable String deviceId,
            @Valid @RequestBody TableUpdateRequest request
    ) {
        return ResponseEntity.ok(tableService.updateTable(deviceId, request));
    }

    @PostMapping("/history")
    @Operation(summary = "입장 기록 조회", description = "최근 30일간의 테이블 입장 기록을 offset 기반 페이지네이션으로 조회합니다")
    public ResponseEntity<TableHistoryListResponse> getTableHistory(
            @RequestBody TableHistoryRequest request
    ) {
        return ResponseEntity.ok(tableService.getTableHistory(request));
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "테이블 삭제", description = "테이블을 삭제합니다 (id에 postfix 붙여서 비활성화)")
    public ResponseEntity<Void> deleteTable(@PathVariable String deviceId) {
        tableService.deleteTable(deviceId);
        return ResponseEntity.ok().build();
    }
}
