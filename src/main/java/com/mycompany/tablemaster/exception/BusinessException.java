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

    // 채팅 관련 예외
    public static BusinessException chatRoomNotFound() {
        return new BusinessException("채팅방을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "CHAT_001");
    }

    public static BusinessException chatRoomNotActive() {
        return new BusinessException("활성 상태의 채팅방이 아닙니다", HttpStatus.BAD_REQUEST, "CHAT_002");
    }

    public static BusinessException chatParticipantNotFound() {
        return new BusinessException("채팅 참여자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "CHAT_003");
    }

    public static BusinessException chatReportNotFound() {
        return new BusinessException("신고 내역을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "CHAT_004");
    }

    public static BusinessException tableNotChatEnabled() {
        return new BusinessException("채팅이 허용되지 않은 테이블입니다", HttpStatus.BAD_REQUEST, "CHAT_005");
    }

    public static BusinessException alreadyChatting() {
        return new BusinessException("이미 채팅 중인 테이블입니다", HttpStatus.CONFLICT, "CHAT_006");
    }

    public static BusinessException noActiveChatRoom() {
        return new BusinessException("활성 채팅방이 없습니다", HttpStatus.NOT_FOUND, "CHAT_007");
    }

    public static BusinessException chatRoomAlreadyExists() {
        return new BusinessException("이미 상대방과 진행 중인 채팅방이 있습니다", HttpStatus.CONFLICT, "CHAT_008");
    }

    public static BusinessException chatModerationHistoryNotFound() {
        return new BusinessException("제재 이력을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "CHAT_009");
    }

    public static BusinessException forbiddenWordAlreadyExists() {
        return new BusinessException("이미 등록된 금칙어입니다", HttpStatus.CONFLICT, "CHAT_010");
    }

    public static BusinessException forbiddenWordNotFound() {
        return new BusinessException("금칙어를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "CHAT_011");
    }

    public static BusinessException forbiddenWordDetected(String word) {
        return new BusinessException("금칙어가 포함되어 전송할 수 없습니다: " + word, HttpStatus.BAD_REQUEST, "CHAT_012");
    }

    public static BusinessException chatReportSelfNotAllowed() {
        return new BusinessException("자기 자신은 신고할 수 없습니다", HttpStatus.BAD_REQUEST, "CHAT_013");
    }

    public static BusinessException chatReportParticipantMismatch() {
        return new BusinessException("해당 채팅방 참여자만 신고할 수 있습니다", HttpStatus.BAD_REQUEST, "CHAT_014");
    }

    public static BusinessException chatReportAlreadyPending() {
        return new BusinessException("이미 처리 대기 중인 신고가 있습니다", HttpStatus.CONFLICT, "CHAT_015");
    }

    public static BusinessException tableNotFound() {
        return new BusinessException("테이블을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "TABLE_001");
    }

    public static BusinessException tableHasOpenBill() {
        return new BusinessException("미정산 주문이 있어 테이블을 삭제할 수 없습니다", HttpStatus.CONFLICT, "TABLE_007");
    }
}
