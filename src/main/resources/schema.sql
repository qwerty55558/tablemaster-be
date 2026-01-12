-- =====================================================
-- TableMaster Database Schema (PostgreSQL)
-- Generated from JPA Entities
-- Version: 2.0
-- =====================================================

-- =====================================================
-- DDL: 테이블 생성
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

COMMENT ON TABLE users IS '사용자 정보';
COMMENT ON COLUMN users.email IS '이메일 (로그인 ID)';
COMMENT ON COLUMN users.password IS '암호화된 비밀번호';
COMMENT ON COLUMN users.name IS '사용자 이름';
COMMENT ON COLUMN users.phone IS '전화번호';

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

COMMENT ON TABLE user_roles IS '사용자 역할 매핑';
COMMENT ON COLUMN user_roles.role IS 'ROLE_USER, ROLE_STAFF, ROLE_ADMIN';

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

COMMENT ON TABLE terms IS '약관 정보';
COMMENT ON COLUMN terms.type IS 'SERVICE, PRIVACY, MARKETING';
COMMENT ON COLUMN terms.required IS '필수 동의 여부';
COMMENT ON COLUMN terms.is_active IS '활성화 여부';

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

COMMENT ON TABLE user_terms_agreements IS '사용자 약관 동의 내역';

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

COMMENT ON TABLE refresh_tokens IS 'JWT 리프레시 토큰';
COMMENT ON COLUMN refresh_tokens.revoked IS '폐기 여부';

-- 6. device_whitelist 테이블 (디바이스 화이트리스트)
CREATE TABLE IF NOT EXISTS device_whitelist (
    id              BIGSERIAL       PRIMARY KEY,
    device_id       VARCHAR(255)    NOT NULL UNIQUE,
    device_name     VARCHAR(100),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    last_login_at   TIMESTAMP
);

COMMENT ON TABLE device_whitelist IS '등록된 디바이스 목록';
COMMENT ON COLUMN device_whitelist.device_id IS '디바이스 고유 ID';
COMMENT ON COLUMN device_whitelist.device_name IS '디바이스 이름 (예: 테이블 A1)';
COMMENT ON COLUMN device_whitelist.is_active IS '활성화 여부';

-- 7. notifications 테이블 (알림)
CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL       PRIMARY KEY,
    device_id       VARCHAR(255)    NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    body            TEXT,
    category        VARCHAR(50)     NOT NULL,
    data            TEXT,
    is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
    is_delivered    BOOLEAN         NOT NULL DEFAULT FALSE,
    delivered_at    TIMESTAMP,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE notifications IS '디바이스 알림';
COMMENT ON COLUMN notifications.category IS 'ORDER, CHAT, GIFT, SYSTEM, PROMOTION';
COMMENT ON COLUMN notifications.data IS 'JSON 형식의 추가 데이터';
COMMENT ON COLUMN notifications.is_delivered IS 'WebSocket 전송 여부';

-- 8. tables 테이블 (테이블 정보)
CREATE TABLE IF NOT EXISTS tables (
    id              VARCHAR(50)     PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    status          VARCHAR(50)     NOT NULL DEFAULT 'AVAILABLE',
    location        VARCHAR(255),
    guest_count     INTEGER,
    female_count    INTEGER,
    male_count      INTEGER,
    device_id       VARCHAR(255),
    is_chatting     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE tables IS '매장 테이블 정보';
COMMENT ON COLUMN tables.id IS '테이블 ID (예: A1, B2)';
COMMENT ON COLUMN tables.status IS 'AVAILABLE, OCCUPIED, RESERVED, CHATTING';
COMMENT ON COLUMN tables.location IS '지역';
COMMENT ON COLUMN tables.guest_count IS '총 인원';
COMMENT ON COLUMN tables.female_count IS '여성 인원';
COMMENT ON COLUMN tables.male_count IS '남성 인원';
COMMENT ON COLUMN tables.device_id IS '연결된 디바이스 ID';

-- =====================================================
-- 인덱스 생성
-- =====================================================

-- users 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- refresh_tokens 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires ON refresh_tokens(expires_at);

-- user_terms_agreements 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_user_terms_user_id ON user_terms_agreements(user_id);
CREATE INDEX IF NOT EXISTS idx_user_terms_terms_id ON user_terms_agreements(terms_id);

-- terms 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_terms_type ON terms(type);
CREATE INDEX IF NOT EXISTS idx_terms_is_active ON terms(is_active);

-- device_whitelist 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_device_whitelist_device_id ON device_whitelist(device_id);
CREATE INDEX IF NOT EXISTS idx_device_whitelist_is_active ON device_whitelist(is_active);

-- notifications 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_notification_device_delivered ON notifications(device_id, is_delivered);
CREATE INDEX IF NOT EXISTS idx_notification_device_created ON notifications(device_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_device_read ON notifications(device_id, is_read);

-- tables 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_tables_status ON tables(status);
CREATE INDEX IF NOT EXISTS idx_tables_device_id ON tables(device_id);

-- =====================================================
-- DML: 초기 데이터
-- =====================================================

-- 약관 데이터
INSERT INTO terms (type, title, content, version, required, is_active)
VALUES
    ('SERVICE', '서비스 이용약관', '서비스 이용약관 내용...', '1.0', TRUE, TRUE),
    ('PRIVACY', '개인정보 처리방침', '개인정보 처리방침 내용...', '1.0', TRUE, TRUE),
    ('MARKETING', '마케팅 정보 수신 동의', '마케팅 정보 수신 동의 내용...', '1.0', FALSE, TRUE)
ON CONFLICT DO NOTHING;

-- 기본 테이블 데이터 (A1~A10, B1~B10)
INSERT INTO tables (id, name, status, is_chatting) VALUES
    ('A1', '테이블 A1', 'AVAILABLE', FALSE),
    ('A2', '테이블 A2', 'AVAILABLE', FALSE),
    ('A3', '테이블 A3', 'AVAILABLE', FALSE),
    ('A4', '테이블 A4', 'AVAILABLE', FALSE),
    ('A5', '테이블 A5', 'AVAILABLE', FALSE),
    ('A6', '테이블 A6', 'AVAILABLE', FALSE),
    ('A7', '테이블 A7', 'AVAILABLE', FALSE),
    ('A8', '테이블 A8', 'AVAILABLE', FALSE),
    ('A9', '테이블 A9', 'AVAILABLE', FALSE),
    ('A10', '테이블 A10', 'AVAILABLE', FALSE),
    ('B1', '테이블 B1', 'AVAILABLE', FALSE),
    ('B2', '테이블 B2', 'AVAILABLE', FALSE),
    ('B3', '테이블 B3', 'AVAILABLE', FALSE),
    ('B4', '테이블 B4', 'AVAILABLE', FALSE),
    ('B5', '테이블 B5', 'AVAILABLE', FALSE),
    ('B6', '테이블 B6', 'AVAILABLE', FALSE),
    ('B7', '테이블 B7', 'AVAILABLE', FALSE),
    ('B8', '테이블 B8', 'AVAILABLE', FALSE),
    ('B9', '테이블 B9', 'AVAILABLE', FALSE),
    ('B10', '테이블 B10', 'AVAILABLE', FALSE)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 유틸리티 함수
-- =====================================================

-- updated_at 자동 갱신 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- users 테이블 트리거
DROP TRIGGER IF EXISTS trigger_users_updated_at ON users;
CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- tables 테이블 트리거
DROP TRIGGER IF EXISTS trigger_tables_updated_at ON tables;
CREATE TRIGGER trigger_tables_updated_at
    BEFORE UPDATE ON tables
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 뷰 (View)
-- =====================================================

-- 활성 사용자 뷰
CREATE OR REPLACE VIEW v_active_users AS
SELECT
    u.id,
    u.email,
    u.name,
    u.phone,
    u.created_at,
    string_agg(ur.role, ', ') AS roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
GROUP BY u.id, u.email, u.name, u.phone, u.created_at;

-- 테이블 현황 뷰
CREATE OR REPLACE VIEW v_table_status AS
SELECT
    t.id,
    t.name,
    t.status,
    t.location,
    t.guest_count,
    t.female_count,
    t.male_count,
    t.is_chatting,
    d.device_name,
    t.updated_at
FROM tables t
LEFT JOIN device_whitelist d ON t.device_id = d.device_id;

-- 미전달 알림 뷰
CREATE OR REPLACE VIEW v_undelivered_notifications AS
SELECT
    n.id,
    n.device_id,
    n.title,
    n.category,
    n.created_at
FROM notifications n
WHERE n.is_delivered = FALSE
ORDER BY n.created_at ASC;

-- =====================================================
-- Enum 값 참조
-- =====================================================
/*
Role: ROLE_USER, ROLE_STAFF, ROLE_ADMIN
TermsType: SERVICE, PRIVACY, MARKETING
TableStatus: AVAILABLE, OCCUPIED, RESERVED, CHATTING
NotificationCategory: ORDER, CHAT, GIFT, SYSTEM, PROMOTION
*/
