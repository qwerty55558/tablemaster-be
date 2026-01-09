-- =====================================================
-- TableMaster Database Schema (PostgreSQL)
-- Generated from JPA Entities
-- =====================================================

-- 1. users 테이블
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL       PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password        VARCHAR(255)    NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    phone           VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 2. user_roles 테이블 (User의 ElementCollection)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id         BIGINT          NOT NULL,
    role            VARCHAR(50)     NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE
);

-- Role enum 값: 'ROLE_USER', 'ROLE_STAFF', 'ROLE_ADMIN'

-- 3. terms 테이블 (약관)
CREATE TABLE IF NOT EXISTS terms (
    id              BIGSERIAL       PRIMARY KEY,
    type            VARCHAR(50)     NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    content         TEXT,
    version         VARCHAR(255)    NOT NULL,
    required        BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- TermsType enum 값: 'SERVICE', 'PRIVACY', 'MARKETING'

-- 4. user_terms_agreements 테이블 (사용자 약관 동의)
CREATE TABLE IF NOT EXISTS user_terms_agreements (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    terms_id        BIGINT          NOT NULL,
    agreed_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address      VARCHAR(255),
    CONSTRAINT fk_user_terms_user
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_user_terms_terms
        FOREIGN KEY (terms_id) 
        REFERENCES terms(id) 
        ON DELETE CASCADE
);

-- 5. refresh_tokens 테이블 (리프레시 토큰)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    token           VARCHAR(500)    NOT NULL UNIQUE,
    device_info     VARCHAR(255),
    ip_address      VARCHAR(45),
    expires_at      TIMESTAMP       NOT NULL,
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE
);

-- =====================================================
-- 인덱스 생성
-- =====================================================

-- users 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- refresh_tokens 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_tokens(user_id);

-- user_terms_agreements 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_user_terms_user_id ON user_terms_agreements(user_id);
CREATE INDEX IF NOT EXISTS idx_user_terms_terms_id ON user_terms_agreements(terms_id);

-- terms 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_terms_type ON terms(type);
CREATE INDEX IF NOT EXISTS idx_terms_is_active ON terms(is_active);

-- =====================================================
-- 초기 데이터 (약관)
-- =====================================================

INSERT INTO terms (type, title, content, version, required, is_active) 
VALUES 
    ('SERVICE', '서비스 이용약관', '서비스 이용약관 내용...', '1.0', TRUE, TRUE),
    ('PRIVACY', '개인정보 처리방침', '개인정보 처리방침 내용...', '1.0', TRUE, TRUE),
    ('MARKETING', '마케팅 정보 수신 동의', '마케팅 정보 수신 동의 내용...', '1.0', FALSE, TRUE)
ON CONFLICT DO NOTHING;
