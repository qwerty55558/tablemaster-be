package com.mycompany.tablemaster.dto.table;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "테이블 입장 기록 목록 응답")
public class TableHistoryListResponse {

    @Schema(description = "입장 기록 목록")
    private List<TableHistoryResponse> content;

    @Schema(description = "전체 건수", example = "100")
    private long totalCount;

    @Schema(description = "현재 offset", example = "0")
    private int offset;

    @Schema(description = "조회 개수", example = "20")
    private int limit;

    @Schema(description = "다음 데이터 존재 여부", example = "true")
    private boolean hasNext;
}
