package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.table.TableListResponse;
import com.mycompany.tablemaster.dto.table.TableSetupRequest;
import com.mycompany.tablemaster.dto.table.TableSetupResponse;
import com.mycompany.tablemaster.service.TableService;
import com.mycompany.tablemaster.websocket.DevicePrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/{tableId}")
    @Operation(summary = "테이블 상세 조회", description = "특정 테이블의 상세 정보를 조회합니다")
    public ResponseEntity<TableSetupResponse> getTable(@PathVariable String tableId) {
        return ResponseEntity.ok(tableService.getTable(tableId));
    }

    @GetMapping("/my")
    @Operation(summary = "내 테이블 조회", description = "현재 디바이스에 연결된 테이블 정보를 조회합니다")
    public ResponseEntity<TableSetupResponse> getMyTable(Principal principal) {
        String deviceId = principal.getName();
        return ResponseEntity.ok(tableService.getTableByDeviceId(deviceId));
    }

    @PostMapping("/setup")
    @Operation(summary = "테이블 설정", description = "테이블 입장 시 지역, 인원, 성비 정보를 설정합니다")
    public ResponseEntity<TableSetupResponse> setupTable(
            @Valid @RequestBody TableSetupRequest request,
            Principal principal
    ) {
        String deviceId = principal.getName();
        return ResponseEntity.ok(tableService.setupTable(request, deviceId));
    }

    @PostMapping("/{tableId}/reset")
    @Operation(summary = "테이블 초기화", description = "테이블을 초기 상태로 리셋합니다 (관리자/스태프용)")
    public ResponseEntity<Void> resetTable(@PathVariable String tableId) {
        tableService.resetTable(tableId);
        return ResponseEntity.ok().build();
    }
}
