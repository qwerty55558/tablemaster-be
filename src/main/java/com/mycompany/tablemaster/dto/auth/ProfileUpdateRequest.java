package com.mycompany.tablemaster.dto.auth;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProfileUpdateRequest {

    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요")
    private String name;

    @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "올바른 전화번호 형식이 아닙니다")
    private String phone;

    private String profileImageUrl;
}
