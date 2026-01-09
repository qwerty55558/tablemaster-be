package com.mycompany.tablemaster.dto.table;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "테이블 설정 요청")
public class TableSetupRequest {

    @NotBlank(message = "테이블 ID는 필수입니다")
    @Schema(description = "테이블 ID", example = "A1")
    private String tableId;

    @NotBlank(message = "지역은 필수입니다")
    @Schema(description = "지역", example = "서울")
    private String location;

    @NotNull(message = "총 인원은 필수입니다")
    @Min(value = 1, message = "인원은 최소 1명 이상이어야 합니다")
    @Schema(description = "총 인원", example = "6")
    private Integer guestCount;

    @NotNull(message = "여성 인원은 필수입니다")
    @Min(value = 0, message = "여성 인원은 0명 이상이어야 합니다")
    @Schema(description = "여성 인원", example = "3")
    private Integer femaleCount;

    @NotNull(message = "남성 인원은 필수입니다")
    @Min(value = 0, message = "남성 인원은 0명 이상이어야 합니다")
    @Schema(description = "남성 인원", example = "3")
    private Integer maleCount;
}
