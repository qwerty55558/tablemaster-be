package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.config.properties.AuthValidationProperties;
import com.mycompany.tablemaster.config.properties.JwtProperties;
import com.mycompany.tablemaster.dto.auth.*;
import com.mycompany.tablemaster.entity.RefreshToken;
import com.mycompany.tablemaster.entity.Terms;
import com.mycompany.tablemaster.entity.Terms.TermsType;
import com.mycompany.tablemaster.entity.User;
import com.mycompany.tablemaster.entity.UserTermsAgreement;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.repository.RefreshTokenRepository;
import com.mycompany.tablemaster.repository.TermsRepository;
import com.mycompany.tablemaster.repository.UserRepository;
import com.mycompany.tablemaster.repository.UserTermsAgreementRepository;
import com.mycompany.tablemaster.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final TermsRepository termsRepository;
    private final UserTermsAgreementRepository userTermsAgreementRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthValidationProperties validationProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 이메일 중복 확인
     * debounce된 실시간 요청을 위한 가벼운 조회
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Transactional
    public SignUpResponse signUp(SignUpRequest request, String ipAddress) {
        // 1. 유효성 검사
        validateSignUpRequest(request);

        // 2. 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.duplicateEmail();
        }

        // 3. 필수 약관 동의 체크
        if (!Boolean.TRUE.equals(request.getAgreeService()) || !Boolean.TRUE.equals(request.getAgreePrivacy())) {
            throw BusinessException.requiredTermsNotAgreed();
        }

        // 4. 사용자 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(normalizePhone(request.getPhone()))
                .build();

        User savedUser = userRepository.save(user);

        // 4. 약관 동의 이력 저장
        saveTermsAgreement(savedUser, TermsType.SERVICE, true, ipAddress);
        saveTermsAgreement(savedUser, TermsType.PRIVACY, true, ipAddress);

        if (Boolean.TRUE.equals(request.getAgreeMarketing())) {
            saveTermsAgreement(savedUser, TermsType.MARKETING, true, ipAddress);
        }

        return SignUpResponse.from(savedUser);
    }

    private void saveTermsAgreement(User user, TermsType termsType, boolean agreed, String ipAddress) {
        if (!agreed) return;

        Terms terms = termsRepository.findByTypeAndIsActiveTrue(termsType)
                .orElseThrow(BusinessException::termsNotFound);

        UserTermsAgreement agreement = UserTermsAgreement.builder()
                .user(user)
                .terms(terms)
                .ipAddress(ipAddress)
                .build();

        userTermsAgreementRepository.save(agreement);
    }

    private String normalizePhone(String phone) {
        return phone.replaceAll("-", "");
    }

    private void validateSignUpRequest(SignUpRequest request) {
        var emailConfig = validationProperties.getEmail();
        var passwordConfig = validationProperties.getPassword();
        var nameConfig = validationProperties.getName();

        if (request.getEmail().length() > emailConfig.getMaxLength()) {
            throw BusinessException.invalidEmailLength(emailConfig.getMaxLength());
        }

        String password = request.getPassword();
        if (password.length() < passwordConfig.getMinLength() || password.length() > passwordConfig.getMaxLength()) {
            throw BusinessException.invalidPasswordLength(passwordConfig.getMinLength(), passwordConfig.getMaxLength());
        }
        if (passwordConfig.isRequireUppercase() && !password.matches(".*[A-Z].*")) {
            throw BusinessException.passwordRequiresUppercase();
        }
        if (passwordConfig.isRequireLowercase() && !password.matches(".*[a-z].*")) {
            throw BusinessException.passwordRequiresLowercase();
        }
        if (passwordConfig.isRequireNumber() && !password.matches(".*[0-9].*")) {
            throw BusinessException.passwordRequiresNumber();
        }
        if (passwordConfig.isRequireSpecialChar() && !password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            throw BusinessException.passwordRequiresSpecialChar();
        }

        String name = request.getName();
        if (name.length() < nameConfig.getMinLength() || name.length() > nameConfig.getMaxLength()) {
            throw BusinessException.invalidNameLength(nameConfig.getMinLength(), nameConfig.getMaxLength());
        }
    }

    // ==================== 로그인/토큰 관련 ====================

    /**
     * 로그인 결과 (토큰 정보만)
     */
    public record LoginResult(
            String accessToken,
            String refreshToken,
            long expiresIn
    ) {}

    /**
     * 로그인
     */
    @Transactional
    public LoginResult login(LoginRequest request, String ipAddress, String deviceInfo) {
        // 1. 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(BusinessException::invalidCredentials);

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.invalidCredentials();
        }

        // 3. 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getEmail(), user.getName(), user.getRoles());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 4. Refresh Token DB 저장
        saveRefreshToken(user, refreshToken, ipAddress, deviceInfo);

        log.info("User logged in: userId={}, email={}", user.getId(), user.getEmail());

        return new LoginResult(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationInSeconds()
        );
    }

    /**
     * 토큰 갱신 (refreshToken 문자열로 직접 받음)
     */
    @Transactional
    public TokenRefreshResponse refresh(String refreshToken) {
        // 1. 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw BusinessException.invalidRefreshToken();
        }

        // 2. Refresh Token인지 확인
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw BusinessException.invalidRefreshToken();
        }

        // 3. DB에서 토큰 조회
        RefreshToken storedToken = refreshTokenRepository
                .findValidToken(refreshToken, LocalDateTime.now())
                .orElseThrow(BusinessException::invalidRefreshToken);

        // 4. 사용자 조회
        User user = storedToken.getUser();

        // 5. 새 Access Token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getEmail(), user.getName(), user.getRoles());

        log.info("Token refreshed: userId={}", user.getId());

        return TokenRefreshResponse.of(
                newAccessToken,
                jwtTokenProvider.getAccessTokenExpirationInSeconds()
        );
    }

    /**
     * 로그아웃 (Access Token 블랙리스트 + Refresh Token 무효화)
     * 
     * @param accessToken 현재 Access Token (블랙리스트 등록용, null 가능)
     * @param refreshToken Refresh Token (DB에서 무효화, null 가능)
     */
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // 1. Access Token 블랙리스트 등록
        blacklistAccessToken(accessToken);

        // 2. Refresh Token 무효화 (DB)
        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken)
                    .ifPresent(token -> {
                        token.revoke();
                        log.info("User logged out: userId={}", token.getUser().getId());
                    });
        }
    }

    /**
     * 전체 로그아웃 (현재 Access Token 블랙리스트 + 모든 Refresh Token 무효화)
     * 
     * @param userId 사용자 ID
     * @param accessToken 현재 Access Token (블랙리스트 등록용, null 가능)
     */
    @Transactional
    public void logoutAll(Long userId, String accessToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException::userNotFound);

        // 1. Access Token 블랙리스트 등록
        blacklistAccessToken(accessToken);

        // 2. 모든 Refresh Token 무효화 (DB)
        refreshTokenRepository.revokeAllByUser(user);
        log.info("User logged out from all devices: userId={}", userId);
    }

    /**
     * Access Token 블랙리스트 등록 (내부 헬퍼 메서드)
     */
    private void blacklistAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }

        try {
            // 토큰 유효성 체크 (만료된 토큰은 블랙리스트 불필요)
            if (!jwtTokenProvider.validateToken(accessToken)) {
                log.debug("Token is invalid or expired, skipping blacklist");
                return;
            }

            String jti = jwtTokenProvider.getJti(accessToken);
            long remainingMs = jwtTokenProvider.getRemainingExpiration(accessToken);

            if (jti != null && remainingMs > 0) {
                tokenBlacklistService.blacklist(jti, remainingMs);
            }
        } catch (Exception e) {
            log.warn("Failed to blacklist access token: {}", e.getMessage());
        }
    }

    // ==================== 비밀번호 변경 ====================

    /**
     * 비밀번호 변경 결과
     */
    public record ChangePasswordResult(
            String newAccessToken,
            String newRefreshToken,
            long expiresIn
    ) {}

    /**
     * 비밀번호 변경
     * 
     * @param userId 사용자 ID
     * @param currentPassword 현재 비밀번호
     * @param newPassword 새 비밀번호
     * @param currentAccessToken 현재 Access Token (블랙리스트용)
     * @param logoutOtherDevices 다른 기기 로그아웃 여부
     * @param ipAddress IP 주소 (새 Refresh Token용)
     * @param deviceInfo 디바이스 정보
     * @return 새 토큰 정보
     */
    @Transactional
    public ChangePasswordResult changePassword(
            Long userId,
            String currentPassword,
            String newPassword,
            String currentAccessToken,
            boolean logoutOtherDevices,
            String ipAddress,
            String deviceInfo
    ) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException::userNotFound);

        // 2. 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw BusinessException.invalidCurrentPassword();
        }

        // 3. 새 비밀번호 유효성 검사
        validateNewPassword(newPassword);

        // 4. 비밀번호 변경
        user.changePassword(passwordEncoder.encode(newPassword));

        // 5. 현재 Access Token 블랙리스트 등록
        blacklistAccessToken(currentAccessToken);

        // 6. 모든 Refresh Token 무효화 (다른 기기 로그아웃)
        if (logoutOtherDevices) {
            refreshTokenRepository.revokeAllByUser(user);
            log.info("Password changed, all refresh tokens revoked: userId={}", userId);
        }

        // 7. 새 토큰 발급 (현재 기기용)
        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getEmail(), user.getName(), user.getRoles());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 8. 새 Refresh Token 저장
        saveRefreshToken(user, newRefreshToken, ipAddress, deviceInfo);

        log.info("Password changed successfully: userId={}", userId);

        return new ChangePasswordResult(
                newAccessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpirationInSeconds()
        );
    }

    /**
     * 새 비밀번호 유효성 검사
     */
    private void validateNewPassword(String password) {
        var passwordConfig = validationProperties.getPassword();

        if (password.length() < passwordConfig.getMinLength() || 
            password.length() > passwordConfig.getMaxLength()) {
            throw BusinessException.invalidPasswordLength(
                    passwordConfig.getMinLength(), 
                    passwordConfig.getMaxLength()
            );
        }
        if (passwordConfig.isRequireUppercase() && !password.matches(".*[A-Z].*")) {
            throw BusinessException.passwordRequiresUppercase();
        }
        if (passwordConfig.isRequireLowercase() && !password.matches(".*[a-z].*")) {
            throw BusinessException.passwordRequiresLowercase();
        }
        if (passwordConfig.isRequireNumber() && !password.matches(".*[0-9].*")) {
            throw BusinessException.passwordRequiresNumber();
        }
        if (passwordConfig.isRequireSpecialChar() && !password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            throw BusinessException.passwordRequiresSpecialChar();
        }
    }

    /**
     * Refresh Token 저장
     */
    private void saveRefreshToken(User user, String token, String ipAddress, String deviceInfo) {
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtProperties.getRefreshTokenExpirationInSeconds());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);
    }
}
