package com.mycompany.tablemaster.dto.table;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "테이블 수정 요청")
public class TableUpdateRequest {

    @Schema(description = "지역", example = "서울")
    private String location;

    @Min(value = 1, message = "총 인원은 1명 이상이어야 합니다")
    @Schema(description = "총 인원", example = "6")
    private Integer guestCount;

    @Min(value = 0, message = "여성 인원은 0명 이상이어야 합니다")
    @Schema(description = "여성 인원", example = "3")
    private Integer femaleCount;

    @Min(value = 0, message = "남성 인원은 0명 이상이어야 합니다")
    @Schema(description = "남성 인원", example = "3")
    private Integer maleCount;

    @Schema(description = "채팅 중 여부", example = "true")
    private Boolean isChatting;
}
