-- TableMaster Database Initialization Script
-- PostgreSQL

-- ========================================
-- Users Table
-- ========================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ========================================
-- User Roles (ElementCollection)
-- ========================================
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- ========================================
-- Refresh Tokens Table
-- ========================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_tokens(user_id);

-- ========================================
-- Tables (TableEntity)
-- ========================================
CREATE TABLE IF NOT EXISTS tables (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    location VARCHAR(255),
    guest_count INTEGER,
    female_count INTEGER,
    male_count INTEGER,
    revenue BIGINT DEFAULT 0,
    is_chatting BOOLEAN NOT NULL DEFAULT FALSE,
    previous_status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ========================================
-- Device Whitelist Table
-- ========================================
CREATE TABLE IF NOT EXISTS device_whitelist (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL UNIQUE,
    device_name VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    last_login_at TIMESTAMP
);

-- ========================================
-- Notifications Table
-- ========================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    category VARCHAR(50) NOT NULL,
    data TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_delivered BOOLEAN NOT NULL DEFAULT FALSE,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_device_delivered ON notifications(device_id, is_delivered);
CREATE INDEX IF NOT EXISTS idx_notification_device_created ON notifications(device_id, created_at DESC);

-- ========================================
-- Table History
-- ========================================
CREATE TABLE IF NOT EXISTS table_history (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    guest_count INTEGER,
    female_count INTEGER,
    male_count INTEGER,
    revenue BIGINT,
    created_at TIMESTAMP,
    deleted_at TIMESTAMP
);

-- ========================================
-- Terms Table
-- ========================================
CREATE TABLE IF NOT EXISTS terms (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    version VARCHAR(50) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP
);

-- ========================================
-- User Terms Agreements
-- ========================================
CREATE TABLE IF NOT EXISTS user_terms_agreements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    terms_id BIGINT NOT NULL REFERENCES terms(id) ON DELETE CASCADE,
    agreed_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(45)
);