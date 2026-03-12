-- ============================================
-- 초기 데이터 (PostgreSQL)
-- ============================================

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
