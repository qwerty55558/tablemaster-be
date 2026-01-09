package com.mycompany.tablemaster.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BusinessException duplicateEmail() {
        return new BusinessException("이미 사용 중인 이메일입니다", HttpStatus.CONFLICT, "AUTH_001");
    }

    public static BusinessException requiredTermsNotAgreed() {
        return new BusinessException("필수 약관에 동의해주세요", HttpStatus.BAD_REQUEST, "AUTH_002");
    }

    public static BusinessException termsNotFound() {
        return new BusinessException("약관 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "AUTH_003");
    }

    public static BusinessException invalidEmailLength(int maxLength) {
        return new BusinessException("이메일은 " + maxLength + "자 이하로 입력해주세요", HttpStatus.BAD_REQUEST, "AUTH_004");
    }

    public static BusinessException invalidPasswordLength(int minLength, int maxLength) {
        return new BusinessException("비밀번호는 " + minLength + "자 이상 " + maxLength + "자 이하로 입력해주세요", HttpStatus.BAD_REQUEST, "AUTH_005");
    }

    public static BusinessException invalidNameLength(int minLength, int maxLength) {
        return new BusinessException("이름은 " + minLength + "자 이상 " + maxLength + "자 이하로 입력해주세요", HttpStatus.BAD_REQUEST, "AUTH_006");
    }

    public static BusinessException passwordRequiresUppercase() {
        return new BusinessException("비밀번호에 대문자를 포함해주세요", HttpStatus.BAD_REQUEST, "AUTH_007");
    }

    public static BusinessException passwordRequiresLowercase() {
        return new BusinessException("비밀번호에 소문자를 포함해주세요", HttpStatus.BAD_REQUEST, "AUTH_008");
    }

    public static BusinessException passwordRequiresNumber() {
        return new BusinessException("비밀번호에 숫자를 포함해주세요", HttpStatus.BAD_REQUEST, "AUTH_009");
    }

    public static BusinessException passwordRequiresSpecialChar() {
        return new BusinessException("비밀번호에 특수문자를 포함해주세요", HttpStatus.BAD_REQUEST, "AUTH_010");
    }

    // 로그인 관련 예외
    public static BusinessException userNotFound() {
        return new BusinessException("존재하지 않는 사용자입니다", HttpStatus.NOT_FOUND, "AUTH_011");
    }

    public static BusinessException invalidPassword() {
        return new BusinessException("비밀번호가 일치하지 않습니다", HttpStatus.UNAUTHORIZED, "AUTH_012");
    }

    public static BusinessException invalidCredentials() {
        return new BusinessException("이메일 또는 비밀번호가 올바르지 않습니다", HttpStatus.UNAUTHORIZED, "AUTH_013");
    }

    // 토큰 관련 예외
    public static BusinessException invalidRefreshToken() {
        return new BusinessException("유효하지 않은 Refresh Token입니다", HttpStatus.UNAUTHORIZED, "AUTH_014");
    }

    public static BusinessException expiredRefreshToken() {
        return new BusinessException("만료된 Refresh Token입니다", HttpStatus.UNAUTHORIZED, "AUTH_015");
    }

    public static BusinessException revokedRefreshToken() {
        return new BusinessException("폐기된 Refresh Token입니다", HttpStatus.UNAUTHORIZED, "AUTH_016");
    }

    // Rate Limiting 관련 예외
    public static BusinessException tooManyLoginAttempts(int remainingSeconds) {
        return new BusinessException(
                "로그인 시도 횟수를 초과했습니다. " + remainingSeconds + "초 후에 다시 시도해주세요",
                HttpStatus.TOO_MANY_REQUESTS,
                "AUTH_017"
        );
    }

    public static BusinessException tooManyRefreshAttempts() {
        return new BusinessException(
                "토큰 갱신 요청이 너무 많습니다. 잠시 후 다시 시도해주세요",
                HttpStatus.TOO_MANY_REQUESTS,
                "AUTH_018"
        );
    }

    // 비밀번호 변경 관련 예외
    public static BusinessException invalidCurrentPassword() {
        return new BusinessException("현재 비밀번호가 일치하지 않습니다", HttpStatus.BAD_REQUEST, "AUTH_019");
    }

    // 디바이스 인증 관련 예외
    public static BusinessException invalidAppSecret() {
        return new BusinessException("인증 정보가 올바르지 않습니다", HttpStatus.UNAUTHORIZED, "DEVICE_001");
    }

    public static BusinessException deviceNotFound() {
        return new BusinessException("등록되지 않은 디바이스입니다", HttpStatus.UNAUTHORIZED, "DEVICE_002");
    }

    public static BusinessException deviceInactive() {
        return new BusinessException("비활성화된 디바이스입니다", HttpStatus.UNAUTHORIZED, "DEVICE_003");
    }
}
