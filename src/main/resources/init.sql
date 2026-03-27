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
    profile_image_url VARCHAR(500),
    email_notification_enabled BOOLEAN NOT NULL DEFAULT true,
    push_notification_enabled BOOLEAN NOT NULL DEFAULT true,
    marketing_notification_enabled BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_notification_enabled BOOLEAN;
ALTER TABLE users ADD COLUMN IF NOT EXISTS push_notification_enabled BOOLEAN;
ALTER TABLE users ADD COLUMN IF NOT EXISTS marketing_notification_enabled BOOLEAN;

UPDATE users
SET email_notification_enabled = true
WHERE email_notification_enabled IS NULL;

UPDATE users
SET push_notification_enabled = true
WHERE push_notification_enabled IS NULL;

UPDATE users
SET marketing_notification_enabled = false
WHERE marketing_notification_enabled IS NULL;

ALTER TABLE users ALTER COLUMN email_notification_enabled SET DEFAULT true;
ALTER TABLE users ALTER COLUMN push_notification_enabled SET DEFAULT true;
ALTER TABLE users ALTER COLUMN marketing_notification_enabled SET DEFAULT false;

ALTER TABLE users ALTER COLUMN email_notification_enabled SET NOT NULL;
ALTER TABLE users ALTER COLUMN push_notification_enabled SET NOT NULL;
ALTER TABLE users ALTER COLUMN marketing_notification_enabled SET NOT NULL;

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

-- 방문자/입장 분석 로그
CREATE TABLE IF NOT EXISTS visitor_analytics_logs (
    id           BIGSERIAL    PRIMARY KEY,
    event_type   VARCHAR(30)  NOT NULL,
    device_id    VARCHAR(255) NOT NULL,
    table_name   VARCHAR(255),
    device_name  VARCHAR(255),
    location     VARCHAR(255),
    guest_count  INTEGER,
    female_count INTEGER,
    male_count   INTEGER,
    revenue      BIGINT,
    table_status VARCHAR(20),
    reason       VARCHAR(255),
    logged_at    TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_visitor_analytics_logged_at
    ON visitor_analytics_logs (logged_at DESC);
CREATE INDEX IF NOT EXISTS idx_visitor_analytics_location_logged_at
    ON visitor_analytics_logs (location, logged_at DESC);
CREATE INDEX IF NOT EXISTS idx_visitor_analytics_event_logged_at
    ON visitor_analytics_logs (event_type, logged_at DESC);

-- 채팅방
CREATE TABLE IF NOT EXISTS chat_rooms (
    id                  BIGSERIAL    PRIMARY KEY,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    sanction_type       VARCHAR(20),
    sanction_reason     VARCHAR(500),
    sanction_expires_at TIMESTAMP,
    started_at          TIMESTAMP,
    closed_at           TIMESTAMP,
    total_message_count INTEGER      NOT NULL DEFAULT 0,
    gift_count          INTEGER      NOT NULL DEFAULT 0,
    report_count        INTEGER      NOT NULL DEFAULT 0
);

-- 채팅 분석 로그
CREATE TABLE IF NOT EXISTS chat_analytics_logs (
    id                  BIGSERIAL    PRIMARY KEY,
    event_type          VARCHAR(30)  NOT NULL,
    chat_room_id        BIGINT       NOT NULL,
    actor_device_id     VARCHAR(255),
    partner_device_id   VARCHAR(255),
    message_id          BIGINT,
    report_id           BIGINT,
    message_type        VARCHAR(30),
    total_message_count INTEGER,
    gift_count          INTEGER,
    report_count        INTEGER,
    reason              VARCHAR(255),
    logged_at           TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_analytics_logged_at
    ON chat_analytics_logs (logged_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_analytics_event_logged_at
    ON chat_analytics_logs (event_type, logged_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_analytics_room_logged_at
    ON chat_analytics_logs (chat_room_id, logged_at DESC);

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
    reporter_table_name VARCHAR(255),
    reported_device_id VARCHAR(255) NOT NULL,
    reported_table_name VARCHAR(255),
    reason             VARCHAR(255) NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reviewed_by        BIGINT,
    reviewed_at        TIMESTAMP,
    created_at         TIMESTAMP
);

ALTER TABLE chat_reports ADD COLUMN IF NOT EXISTS reporter_table_name VARCHAR(255);
ALTER TABLE chat_reports ADD COLUMN IF NOT EXISTS reported_table_name VARCHAR(255);

ALTER TABLE chat_room_participants DROP CONSTRAINT IF EXISTS chat_room_participants_chat_room_id_fkey;
ALTER TABLE chat_room_participants
    ADD CONSTRAINT chat_room_participants_chat_room_id_fkey
    FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE;

ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chat_messages_chat_room_id_fkey;
ALTER TABLE chat_messages
    ADD CONSTRAINT chat_messages_chat_room_id_fkey
    FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE;

ALTER TABLE chat_reports DROP CONSTRAINT IF EXISTS chat_reports_chat_room_id_fkey;
ALTER TABLE chat_reports
    ADD CONSTRAINT chat_reports_chat_room_id_fkey
    FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE;

-- 채팅방 히스토리
CREATE TABLE IF NOT EXISTS chat_room_histories (
    id                  BIGSERIAL    PRIMARY KEY,
    room_id             BIGINT       NOT NULL,
    participants        VARCHAR(255) NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    sanction_type       VARCHAR(20),
    sanction_reason     VARCHAR(500),
    total_message_count INTEGER      NOT NULL,
    gift_count          INTEGER      NOT NULL,
    report_count        INTEGER      NOT NULL,
    started_at          TIMESTAMP,
    closed_at           TIMESTAMP,
    deleted_at          TIMESTAMP,
    delete_reason       VARCHAR(255) NOT NULL
);

-- 채팅 모니터링 로그
CREATE TABLE IF NOT EXISTS chat_monitor_logs (
    id                BIGSERIAL    PRIMARY KEY,
    event_type        VARCHAR(50)  NOT NULL,
    chat_room_id      BIGINT       NOT NULL,
    message_id        BIGINT,
    report_id         BIGINT,
    actor_user_id     BIGINT,
    actor_device_id   VARCHAR(255),
    actor_table_name  VARCHAR(255),
    target_device_id  VARCHAR(255),
    target_table_name VARCHAR(255),
    content           TEXT,
    reason            VARCHAR(500),
    payload           TEXT,
    logged_at         TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_monitor_logs_room_logged_at
    ON chat_monitor_logs (chat_room_id, logged_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_monitor_logs_event_logged_at
    ON chat_monitor_logs (event_type, logged_at DESC);

-- 채팅 제재 이력
CREATE TABLE IF NOT EXISTS chat_moderation_histories (
    id                   BIGSERIAL    PRIMARY KEY,
    chat_room_id         BIGINT       NOT NULL,
    table_names          VARCHAR(255) NOT NULL,
    action_type          VARCHAR(50)  NOT NULL,
    reason               VARCHAR(500),
    action_detail        TEXT,
    report_id            BIGINT,
    processed_by_user_id BIGINT,
    processed_by_name    VARCHAR(255),
    processed_at         TIMESTAMP    NOT NULL,
    payload              TEXT
);

CREATE INDEX IF NOT EXISTS idx_chat_moderation_histories_processed_at
    ON chat_moderation_histories (processed_at DESC);

-- 금칙어
CREATE TABLE IF NOT EXISTS forbidden_words (
    id                 BIGSERIAL    PRIMARY KEY,
    word               VARCHAR(255) NOT NULL UNIQUE,
    reason             VARCHAR(500),
    is_active          BOOLEAN      NOT NULL DEFAULT true,
    created_by_user_id BIGINT,
    created_by_name    VARCHAR(255),
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP
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
    image_url    VARCHAR(500),
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
    image_url    VARCHAR(500),
    is_available BOOLEAN      NOT NULL DEFAULT true
);

ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
ALTER TABLE gift_types ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);

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

CREATE TABLE IF NOT EXISTS gift_orders (
    id            BIGSERIAL    PRIMARY KEY,
    bill_id        BIGINT       NOT NULL REFERENCES bills(id),
    gift_type_id   BIGINT       NOT NULL REFERENCES gift_types(id),
    code           VARCHAR(255) NOT NULL,
    display_name   VARCHAR(255) NOT NULL,
    price          INTEGER      NOT NULL,
    quantity       INTEGER      NOT NULL,
    chat_room_id   BIGINT,
    created_at     TIMESTAMP
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
INSERT INTO menu_items (name, price, category, image_url, is_available, created_at, updated_at) VALUES
('치킨 너겟', 12000, 'FOOD', '/images/menu/food-fried.svg', true, NOW(), NOW()),
('감바스', 15000, 'FOOD', '/images/menu/food-seafood.svg', true, NOW(), NOW()),
('모듬 소시지', 13000, 'FOOD', '/images/menu/food-platter.svg', true, NOW(), NOW()),
('시저 샐러드', 10000, 'FOOD', '/images/menu/food-salad.svg', true, NOW(), NOW()),
('트러플 감자튀김', 9000, 'FOOD', '/images/menu/food-fried.svg', true, NOW(), NOW()),
('나초 플래터', 14000, 'FOOD', '/images/menu/food-platter.svg', true, NOW(), NOW()),
('마르게리타 피자', 16000, 'FOOD', '/images/menu/food-pizza.svg', true, NOW(), NOW()),
('떡볶이', 8000, 'FOOD', '/images/menu/food-spicy.svg', true, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 메뉴: 주류/음료
INSERT INTO menu_items (name, price, category, image_url, is_available, created_at, updated_at) VALUES
('카스 생맥주', 5000, 'DRINK', '/images/menu/drink-beer.svg', true, NOW(), NOW()),
('테라 생맥주', 5000, 'DRINK', '/images/menu/drink-beer.svg', true, NOW(), NOW()),
('클라우드 생맥주', 6000, 'DRINK', '/images/menu/drink-beer.svg', true, NOW(), NOW()),
('소주', 5000, 'DRINK', '/images/menu/drink-bottle.svg', true, NOW(), NOW()),
('하이볼', 8000, 'DRINK', '/images/menu/drink-cocktail.svg', true, NOW(), NOW()),
('모히토', 10000, 'DRINK', '/images/menu/drink-cocktail.svg', true, NOW(), NOW()),
('롱아일랜드', 11000, 'DRINK', '/images/menu/drink-cocktail.svg', true, NOW(), NOW()),
('레드 와인 (잔)', 12000, 'DRINK', '/images/menu/drink-wine-red.svg', true, NOW(), NOW()),
('화이트 와인 (잔)', 12000, 'DRINK', '/images/menu/drink-wine-white.svg', true, NOW(), NOW()),
('콜라', 3000, 'DRINK', '/images/menu/drink-soda-dark.svg', true, NOW(), NOW()),
('사이다', 3000, 'DRINK', '/images/menu/drink-soda-light.svg', true, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 선물 타입
INSERT INTO gift_types (code, display_name, price, image_url, is_available) VALUES
('ROSE', '장미', 3000, '/images/gifts/gift-rose.svg', true),
('CHAMPAGNE', '샴페인', 10000, '/images/gifts/gift-champagne.svg', true),
('CAKE', '케이크', 7000, '/images/gifts/gift-cake.svg', true),
('BEER', '맥주 한잔', 5000, '/images/gifts/gift-beer.svg', true),
('HEART', '하트', 1000, '/images/gifts/gift-heart.svg', true)
ON CONFLICT DO NOTHING;

UPDATE menu_items
SET image_url = CASE
    WHEN category = 'FOOD' AND name IN ('치킨 너겟', '트러플 감자튀김') THEN '/images/menu/food-fried.svg'
    WHEN category = 'FOOD' AND name = '감바스' THEN '/images/menu/food-seafood.svg'
    WHEN category = 'FOOD' AND name IN ('모듬 소시지', '나초 플래터') THEN '/images/menu/food-platter.svg'
    WHEN category = 'FOOD' AND name = '시저 샐러드' THEN '/images/menu/food-salad.svg'
    WHEN category = 'FOOD' AND name = '마르게리타 피자' THEN '/images/menu/food-pizza.svg'
    WHEN category = 'FOOD' AND name = '떡볶이' THEN '/images/menu/food-spicy.svg'
    WHEN category = 'DRINK' AND name IN ('카스 생맥주', '테라 생맥주', '클라우드 생맥주') THEN '/images/menu/drink-beer.svg'
    WHEN category = 'DRINK' AND name = '소주' THEN '/images/menu/drink-bottle.svg'
    WHEN category = 'DRINK' AND name IN ('하이볼', '모히토', '롱아일랜드') THEN '/images/menu/drink-cocktail.svg'
    WHEN category = 'DRINK' AND name = '레드 와인 (잔)' THEN '/images/menu/drink-wine-red.svg'
    WHEN category = 'DRINK' AND name = '화이트 와인 (잔)' THEN '/images/menu/drink-wine-white.svg'
    WHEN category = 'DRINK' AND name = '콜라' THEN '/images/menu/drink-soda-dark.svg'
    WHEN category = 'DRINK' AND name = '사이다' THEN '/images/menu/drink-soda-light.svg'
    ELSE image_url
END
WHERE image_url IS NULL;

UPDATE gift_types
SET image_url = CASE code
    WHEN 'ROSE' THEN '/images/gifts/gift-rose.svg'
    WHEN 'CHAMPAGNE' THEN '/images/gifts/gift-champagne.svg'
    WHEN 'CAKE' THEN '/images/gifts/gift-cake.svg'
    WHEN 'BEER' THEN '/images/gifts/gift-beer.svg'
    WHEN 'HEART' THEN '/images/gifts/gift-heart.svg'
    ELSE image_url
END
WHERE image_url IS NULL;
