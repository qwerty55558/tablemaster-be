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
}
