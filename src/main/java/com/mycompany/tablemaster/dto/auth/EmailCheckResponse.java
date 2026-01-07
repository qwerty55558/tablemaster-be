package com.mycompany.tablemaster.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "이메일 중복확인 응답")
public class EmailCheckResponse {

    @Schema(description = "사용 가능 여부", example = "true")
    private boolean available;

    @Schema(description = "메시지", example = "사용 가능한 이메일입니다")
    private String message;

    public static EmailCheckResponse available() {
        return new EmailCheckResponse(true, "사용 가능한 이메일입니다");
    }

    public static EmailCheckResponse duplicated() {
        return new EmailCheckResponse(false, "이미 사용 중인 이메일입니다");
    }
}
