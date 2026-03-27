package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.analytics.AnalyticsSummaryResponse;
import com.mycompany.tablemaster.service.AnalyticsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "통계 조회 API")
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    @GetMapping("/summary")
    @Operation(summary = "통계 요약 조회", description = "방문/채팅 통계를 한 번에 조회합니다. 기본은 최근 7일입니다.")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(analyticsQueryService.getSummary(from, to));
    }
}
