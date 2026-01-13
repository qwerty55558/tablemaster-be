package com.mycompany.tablemaster.dto.table;

import com.mycompany.tablemaster.entity.TableEntity;
import com.mycompany.tablemaster.entity.TableStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "테이블 목록 항목")
public class TableListResponse {

    @Schema(description = "테이블 ID", example = "A1")
    private String id;

    @Schema(description = "테이블 이름", example = "A1")
    private String name;

    @Schema(description = "테이블 상태", example = "OCCUPIED")
    private TableStatus status;

    @Schema(description = "지역", example = "서울")
    private String location;

    @Schema(description = "총 인원", example = "6")
    private Integer guestCount;

    @Schema(description = "여성 인원", example = "3")
    private Integer femaleCount;

    @Schema(description = "남성 인원", example = "3")
    private Integer maleCount;

    @Schema(description = "채팅 중 여부", example = "false")
    private Boolean isChatting;

    @Schema(description = "마지막 업데이트 시간")
    private LocalDateTime updatedAt;

    public static TableListResponse from(TableEntity table) {
        return TableListResponse.builder()
                .id(table.getId())
                .name(table.getName())
                .status(table.getStatus())
                .location(table.getLocation())
                .guestCount(table.getGuestCount())
                .femaleCount(table.getFemaleCount())
                .maleCount(table.getMaleCount())
                .isChatting(table.getIsChatting())
                .updatedAt(table.getUpdatedAt())
                .build();
    }
}
