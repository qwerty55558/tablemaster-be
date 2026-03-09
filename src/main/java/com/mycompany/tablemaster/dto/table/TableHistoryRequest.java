package com.mycompany.tablemaster.dto.table;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "테이블 입장 기록 조회 요청")
public class TableHistoryRequest {

    @Schema(description = "조회 시작 위치 (offset)", example = "0")
    private int offset = 0;

    @Schema(description = "조회 개수", example = "20")
    private int limit = 20;
}
