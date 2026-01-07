package com.mycompany.tablemaster.dto.config;

import com.mycompany.tablemaster.config.properties.AuthValidationProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "인증 설정 응답")
public class AuthConfigResponse {

    @Schema(description = "이메일 유효성 검사 설정")
    private EmailConfig email;

    @Schema(description = "비밀번호 유효성 검사 설정")
    private PasswordConfig password;

    @Schema(description = "이름 유효성 검사 설정")
    private NameConfig name;

    @Schema(description = "전화번호 유효성 검사 설정")
    private PhoneConfig phone;

    @Getter
    @AllArgsConstructor
    @Schema(description = "이메일 설정")
    public static class EmailConfig {
        @Schema(description = "최대 길이", example = "30")
        private int maxLength;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "비밀번호 설정")
    public static class PasswordConfig {
        @Schema(description = "최소 길이", example = "8")
        private int minLength;

        @Schema(description = "최대 길이", example = "30")
        private int maxLength;

        @Schema(description = "대문자 필수 여부", example = "true")
        private boolean requireUppercase;

        @Schema(description = "소문자 필수 여부", example = "true")
        private boolean requireLowercase;

        @Schema(description = "숫자 필수 여부", example = "true")
        private boolean requireNumber;

        @Schema(description = "특수문자 필수 여부", example = "true")
        private boolean requireSpecialChar;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "이름 설정")
    public static class NameConfig {
        @Schema(description = "최소 길이", example = "2")
        private int minLength;

        @Schema(description = "최대 길이", example = "30")
        private int maxLength;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "전화번호 설정")
    public static class PhoneConfig {
        @Schema(description = "전화번호 패턴", example = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$")
        private String pattern;
    }

    public static AuthConfigResponse from(AuthValidationProperties props) {
        var password = props.getPassword();
        return new AuthConfigResponse(
                new EmailConfig(props.getEmail().getMaxLength()),
                new PasswordConfig(
                        password.getMinLength(),
                        password.getMaxLength(),
                        password.isRequireUppercase(),
                        password.isRequireLowercase(),
                        password.isRequireNumber(),
                        password.isRequireSpecialChar()
                ),
                new NameConfig(
                        props.getName().getMinLength(),
                        props.getName().getMaxLength()
                ),
                new PhoneConfig(props.getPhone().getPattern())
        );
    }
}
