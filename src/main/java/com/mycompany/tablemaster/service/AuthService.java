package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.config.properties.AuthValidationProperties;
import com.mycompany.tablemaster.dto.auth.SignUpRequest;
import com.mycompany.tablemaster.dto.auth.SignUpResponse;
import com.mycompany.tablemaster.entity.Terms;
import com.mycompany.tablemaster.entity.Terms.TermsType;
import com.mycompany.tablemaster.entity.User;
import com.mycompany.tablemaster.entity.UserTermsAgreement;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.repository.TermsRepository;
import com.mycompany.tablemaster.repository.UserRepository;
import com.mycompany.tablemaster.repository.UserTermsAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final TermsRepository termsRepository;
    private final UserTermsAgreementRepository userTermsAgreementRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthValidationProperties validationProperties;

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
}
