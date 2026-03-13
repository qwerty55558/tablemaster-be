-- ============================================
-- 테이블 생성 (PostgreSQL)
-- ============================================

-- 유저
CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    phone      VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 유저 역할 (ElementCollection)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT      NOT NULL REFERENCES users(id),
    role    VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- 약관
CREATE TABLE IF NOT EXISTS terms (
    id         BIGSERIAL    PRIMARY KEY,
    type       VARCHAR(20)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    content    TEXT,
    version    VARCHAR(255) NOT NULL,
    required   BOOLEAN      NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMP
);

-- 유저 약관 동의
CREATE TABLE IF NOT EXISTS user_terms_agreements (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id),
    terms_id   BIGINT       NOT NULL REFERENCES terms(id),
    agreed_at  TIMESTAMP    NOT NULL,
    ip_address VARCHAR(255)
);

-- 리프레시 토큰
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id),
    token       VARCHAR(500) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    ip_address  VARCHAR(45),
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_token   ON refresh_tokens (token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_tokens (user_id);

-- 디바이스 화이트리스트
CREATE TABLE IF NOT EXISTS device_whitelist (
    id            BIGSERIAL    PRIMARY KEY,
    device_id     VARCHAR(255) NOT NULL UNIQUE,
    device_name   VARCHAR(100),
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMP,
    last_login_at TIMESTAMP
);

-- 테이블
CREATE TABLE IF NOT EXISTS tables (
    id              VARCHAR(255) PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    device_name     VARCHAR(255),
    status          VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    location        VARCHAR(255),
    guest_count     INTEGER,
    female_count    INTEGER,
    male_count      INTEGER,
    revenue         BIGINT       DEFAULT 0,
    is_chat_enabled BOOLEAN      NOT NULL DEFAULT false,
    is_chatting     BOOLEAN      NOT NULL DEFAULT false,
    previous_status VARCHAR(20),
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);

-- 테이블 히스토리
CREATE TABLE IF NOT EXISTS table_history (
    id           BIGSERIAL    PRIMARY KEY,
    device_id    VARCHAR(255) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    device_name  VARCHAR(255),
    location     VARCHAR(255),
    guest_count  INTEGER,
    female_count INTEGER,
    male_count   INTEGER,
    revenue      BIGINT,
    created_at   TIMESTAMP,
    deleted_at   TIMESTAMP
);

-- 채팅방
CREATE TABLE IF NOT EXISTS chat_rooms (
    id                  BIGSERIAL   PRIMARY KEY,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at          TIMESTAMP,
    closed_at           TIMESTAMP,
    total_message_count INTEGER     NOT NULL DEFAULT 0,
    gift_count          INTEGER     NOT NULL DEFAULT 0,
    report_count        INTEGER     NOT NULL DEFAULT 0
);

-- 채팅방 참여자
CREATE TABLE IF NOT EXISTS chat_room_participants (
    id           BIGSERIAL    PRIMARY KEY,
    chat_room_id BIGINT       NOT NULL REFERENCES chat_rooms(id),
    device_id    VARCHAR(255) NOT NULL,
    table_name   VARCHAR(255) NOT NULL,
    is_muted     BOOLEAN      NOT NULL DEFAULT false
);

-- 채팅 메시지
CREATE TABLE IF NOT EXISTS chat_messages (
    id                BIGSERIAL    PRIMARY KEY,
    chat_room_id      BIGINT       NOT NULL REFERENCES chat_rooms(id),
    sender_device_id  VARCHAR(255),
    sender_table_name VARCHAR(255),
    content           TEXT,
    type              VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMP
);

-- 채팅 신고
CREATE TABLE IF NOT EXISTS chat_reports (
    id                 BIGSERIAL    PRIMARY KEY,
    chat_room_id       BIGINT       NOT NULL REFERENCES chat_rooms(id),
    reporter_device_id VARCHAR(255) NOT NULL,
    reported_device_id VARCHAR(255) NOT NULL,
    reason             VARCHAR(255) NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reviewed_by        BIGINT,
    created_at         TIMESTAMP
);

-- 채팅방 히스토리
CREATE TABLE IF NOT EXISTS chat_room_histories (
    id                  BIGSERIAL    PRIMARY KEY,
    room_id             BIGINT       NOT NULL,
    participants        VARCHAR(255) NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    total_message_count INTEGER      NOT NULL,
    gift_count          INTEGER      NOT NULL,
    report_count        INTEGER      NOT NULL,
    started_at          TIMESTAMP,
    closed_at           TIMESTAMP,
    deleted_at          TIMESTAMP,
    delete_reason       VARCHAR(255) NOT NULL
);

-- 스태프 채팅 읽음 위치
CREATE TABLE IF NOT EXISTS staff_chat_read_positions (
    id                  BIGSERIAL NOT NULL PRIMARY KEY,
    user_id             BIGINT    NOT NULL,
    chat_room_id        BIGINT    NOT NULL,
    last_read_message_id BIGINT   NOT NULL DEFAULT 0,
    CONSTRAINT uq_staff_chat_read UNIQUE (user_id, chat_room_id)
);

-- 알림
CREATE TABLE IF NOT EXISTS notifications (
    id           BIGSERIAL    PRIMARY KEY,
    device_id    VARCHAR(255) NOT NULL,
    title        VARCHAR(255) NOT NULL,
    body         TEXT,
    category     VARCHAR(50)  NOT NULL,
    data         TEXT,
    is_read      BOOLEAN      NOT NULL DEFAULT false,
    is_delivered BOOLEAN      NOT NULL DEFAULT false,
    delivered_at TIMESTAMP,
    created_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_device_delivered ON notifications (device_id, is_delivered);
CREATE INDEX IF NOT EXISTS idx_notification_device_created   ON notifications (device_id, created_at DESC);

-- 메뉴 아이템
CREATE TABLE IF NOT EXISTS menu_items (
    id           BIGSERIAL    PRIMARY KEY,
    name         VARCHAR(255) NOT NULL UNIQUE,
    price        INTEGER      NOT NULL,
    category     VARCHAR(20)  NOT NULL,
    is_available BOOLEAN      NOT NULL DEFAULT true,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP
);

-- 선물 타입
CREATE TABLE IF NOT EXISTS gift_types (
    id           BIGSERIAL    PRIMARY KEY,
    code         VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    price        INTEGER      NOT NULL,
    is_available BOOLEAN      NOT NULL DEFAULT true
);

-- 빌
CREATE TABLE IF NOT EXISTS bills (
    id           BIGSERIAL    PRIMARY KEY,
    device_id    VARCHAR(255) NOT NULL,
    table_name   VARCHAR(255) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    total_amount BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    closed_at    TIMESTAMP
);

-- 주문 아이템
CREATE TABLE IF NOT EXISTS order_items (
    id           BIGSERIAL   PRIMARY KEY,
    bill_id      BIGINT      NOT NULL REFERENCES bills(id),
    menu_item_id BIGINT      NOT NULL REFERENCES menu_items(id),
    name         VARCHAR(255) NOT NULL,
    price        INTEGER      NOT NULL,
    quantity     INTEGER      NOT NULL,
    category     VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMP
);

-- ============================================
-- 초기 데이터 (PostgreSQL)
-- ============================================

-- 약관
INSERT INTO terms (type, title, content, version, required, is_active, created_at) VALUES
('SERVICE', '서비스 이용약관', '테이블마스터 서비스 이용약관입니다. 본 약관은 회사가 제공하는 서비스의 이용과 관련하여 회사와 이용자의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.', '1.0', true, true, NOW()),
('PRIVACY', '개인정보 처리방침', '테이블마스터 개인정보 처리방침입니다. 회사는 이용자의 개인정보를 중요시하며, 개인정보보호법 등 관련 법령을 준수하고 있습니다.', '1.0', true, true, NOW()),
('MARKETING', '마케팅 정보 수신 동의', '프로모션, 이벤트 등 마케팅 정보를 이메일, SMS 등으로 수신하는 것에 동의합니다.', '1.0', false, true, NOW())
ON CONFLICT DO NOTHING;

-- 관리자 계정: 1@1.com / qweR123$
INSERT INTO users (email, password, name, phone, created_at, updated_at) VALUES
('1@1.com', '$2b$10$YDS91C6W94.TZBP6VRxg4eUtmmpZpoIksYQqULJ60.7Ek3OG7LsqS', '관리자', '010-0000-0000', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_ADMIN' FROM users WHERE email = '1@1.com'
ON CONFLICT DO NOTHING;

-- 관리자 약관 동의
INSERT INTO user_terms_agreements (user_id, terms_id, agreed_at)
SELECT u.id, t.id, NOW() FROM users u, terms t
WHERE u.email = '1@1.com' AND t.type IN ('SERVICE', 'PRIVACY')
ON CONFLICT DO NOTHING;

-- 스태프 계정: 2@2.com / qweR123$
INSERT INTO users (email, password, name, phone, created_at, updated_at) VALUES
('2@2.com', '$2b$10$YDS91C6W94.TZBP6VRxg4eUtmmpZpoIksYQqULJ60.7Ek3OG7LsqS', '스태프', '010-0000-0001', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_STAFF' FROM users WHERE email = '2@2.com'
ON CONFLICT DO NOTHING;

-- 스태프 약관 동의
INSERT INTO user_terms_agreements (user_id, terms_id, agreed_at)
SELECT u.id, t.id, NOW() FROM users u, terms t
WHERE u.email = '2@2.com' AND t.type IN ('SERVICE', 'PRIVACY')
ON CONFLICT DO NOTHING;

-- 메뉴: 음식
INSERT INTO menu_items (name, price, category, is_available, created_at, updated_at) VALUES
('치킨 너겟', 12000, 'FOOD', true, NOW(), NOW()),
('감바스', 15000, 'FOOD', true, NOW(), NOW()),
('모듬 소시지', 13000, 'FOOD', true, NOW(), NOW()),
('시저 샐러드', 10000, 'FOOD', true, NOW(), NOW()),
('트러플 감자튀김', 9000, 'FOOD', true, NOW(), NOW()),
('나초 플래터', 14000, 'FOOD', true, NOW(), NOW()),
('마르게리타 피자', 16000, 'FOOD', true, NOW(), NOW()),
('떡볶이', 8000, 'FOOD', true, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 메뉴: 주류/음료
INSERT INTO menu_items (name, price, category, is_available, created_at, updated_at) VALUES
('카스 생맥주', 5000, 'DRINK', true, NOW(), NOW()),
('테라 생맥주', 5000, 'DRINK', true, NOW(), NOW()),
('클라우드 생맥주', 6000, 'DRINK', true, NOW(), NOW()),
('소주', 5000, 'DRINK', true, NOW(), NOW()),
('하이볼', 8000, 'DRINK', true, NOW(), NOW()),
('모히토', 10000, 'DRINK', true, NOW(), NOW()),
('롱아일랜드', 11000, 'DRINK', true, NOW(), NOW()),
('레드 와인 (잔)', 12000, 'DRINK', true, NOW(), NOW()),
('화이트 와인 (잔)', 12000, 'DRINK', true, NOW(), NOW()),
('콜라', 3000, 'DRINK', true, NOW(), NOW()),
('사이다', 3000, 'DRINK', true, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 선물 타입
INSERT INTO gift_types (code, display_name, price, is_available) VALUES
('ROSE', '장미', 3000, true),
('CHAMPAGNE', '샴페인', 10000, true),
('CAKE', '케이크', 7000, true),
('BEER', '맥주 한잔', 5000, true),
('HEART', '하트', 1000, true)
ON CONFLICT DO NOTHING;
