package com.mycompany.tablemaster.dto.table;

import com.mycompany.tablemaster.entity.TableHistory;
import com.mycompany.tablemaster.entity.TableEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "테이블 입장 기록")
public class TableHistoryResponse {

    @Schema(description = "히스토리 ID", example = "1")
    private Long id;

    @Schema(description = "디바이스 ID", example = "device-001")
    private String deviceId;

    @Schema(description = "테이블 이름", example = "A1")
    private String name;

    @Schema(description = "디바이스 이름", example = "iPad-1")
    private String deviceName;

    @Schema(description = "지역", example = "서울")
    private String location;

    @Schema(description = "총 인원", example = "6")
    private Integer guestCount;

    @Schema(description = "여성 인원", example = "3")
    private Integer femaleCount;

    @Schema(description = "남성 인원", example = "3")
    private Integer maleCount;

    @Schema(description = "매출", example = "150000")
    private Long revenue;

    @Schema(description = "입장 시간")
    private LocalDateTime createdAt;

    @Schema(description = "퇴장 시간")
    private LocalDateTime deletedAt;

    public static TableHistoryResponse from(TableHistory history) {
        return TableHistoryResponse.builder()
                .id(history.getId())
                .deviceId(history.getDeviceId())
                .name(history.getName())
                .deviceName(history.getDeviceName())
                .location(history.getLocation())
                .guestCount(history.getGuestCount())
                .femaleCount(history.getFemaleCount())
                .maleCount(history.getMaleCount())
                .revenue(history.getRevenue())
                .createdAt(history.getCreatedAt())
                .deletedAt(history.getDeletedAt())
                .build();
    }

    public static TableHistoryResponse from(TableEntity table) {
        return TableHistoryResponse.builder()
                .id(null)
                .deviceId(table.getId())
                .name(table.getName())
                .deviceName(table.getDeviceName())
                .location(table.getLocation())
                .guestCount(table.getGuestCount())
                .femaleCount(table.getFemaleCount())
                .maleCount(table.getMaleCount())
                .revenue(table.getRevenue())
                .createdAt(table.getCreatedAt())
                .deletedAt(null)
                .build();
    }
}
