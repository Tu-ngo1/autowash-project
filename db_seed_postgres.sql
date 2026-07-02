-- ============================================================================
-- SQL SCRIPT: RE-CREATE AND SEED TEST DATA FOR AUTOWASH DATABASE (PostgreSQL)
-- Description:
--   1. Safely deletes existing data in constraint-respecting order.
--   2. Resets auto-increment IDENTITY keys.
--   3. Inserts fresh, realistic test data for all 14 tables using explicit IDs
--      to be completely independent of auto-increment start values.
-- Password for all seeded users: password123
-- ============================================================================

BEGIN;

-- ------------------------------------------------------------------------
-- STEP 1: TRUNCATE ALL TABLES AND RESET IDENTITY SEQUENCES
-- ------------------------------------------------------------------------
TRUNCATE TABLE 
    reviews, 
    payments, 
    booking_details, 
    bookings, 
    customer_vouchers, 
    otp_tokens, 
    customer_profiles, 
    cars, 
    promotions, 
    service_prices, 
    users, 
    services, 
    vehicle_models, 
    tier_configs 
RESTART IDENTITY CASCADE;

-- ------------------------------------------------------------------------
-- STEP 2: INSERT FRESH SEED DATA
-- ------------------------------------------------------------------------

-- 2.1. TIER_CONFIGS (Varchar PK, no identity)
INSERT INTO tier_configs (tier_level, is_active, auto_discount_percent, booking_window_days, points_to_maintain, points_to_upgrade)
VALUES 
('MEMBER', true, 0.00, 7, 0, 1000),
('SILVER', true, 2.00, 14, 1000, 3000),
('GOLD', true, 5.00, 21, 3000, 6000),
('PLATINUM', true, 10.00, 30, 6000, 999999);

-- 2.2. VEHICLE_MODELS
INSERT INTO vehicle_models (id, brand, model_name, vehicle_size, is_active, created_at, updated_at) VALUES 
(1, 'Toyota', 'Vios', 'SMALL', true, NOW(), NOW()),
(2, 'Honda', 'Civic', 'MEDIUM', true, NOW(), NOW()),
(3, 'Mazda', '3', 'MEDIUM', true, NOW(), NOW()),
(4, 'Hyundai', 'SantaFe', 'LARGE', true, NOW(), NOW()),
(5, 'Ford', 'Ranger', 'LARGE', true, NOW(), NOW()),
(6, 'Kia', 'Morning', 'SMALL', true, NOW(), NOW()),
(7, 'Toyota', 'Camry', 'MEDIUM', true, NOW(), NOW()),
(8, 'Toyota', 'Corolla Altis', 'MEDIUM', true, NOW(), NOW()),
(9, 'Toyota', 'Corolla Cross', 'MEDIUM', true, NOW(), NOW()),
(10, 'Toyota', 'Fortuner', 'LARGE', true, NOW(), NOW()),
(11, 'Toyota', 'Innova', 'LARGE', true, NOW(), NOW()),
(12, 'Toyota', 'Wigo', 'SMALL', true, NOW(), NOW()),
(13, 'Toyota', 'Raize', 'SMALL', true, NOW(), NOW()),
(14, 'Toyota', 'Hilux', 'LARGE', true, NOW(), NOW()),
(15, 'Honda', 'City', 'SMALL', true, NOW(), NOW()),
(16, 'Honda', 'Accord', 'MEDIUM', true, NOW(), NOW()),
(17, 'Honda', 'CR-V', 'MEDIUM', true, NOW(), NOW()),
(18, 'Honda', 'HR-V', 'MEDIUM', true, NOW(), NOW()),
(19, 'Honda', 'Brio', 'SMALL', true, NOW(), NOW()),
(20, 'Hyundai', 'Grand i10', 'SMALL', true, NOW(), NOW()),
(21, 'Hyundai', 'Accent', 'SMALL', true, NOW(), NOW()),
(22, 'Hyundai', 'Elantra', 'MEDIUM', true, NOW(), NOW()),
(23, 'Hyundai', 'Tucson', 'MEDIUM', true, NOW(), NOW()),
(24, 'Hyundai', 'Creta', 'MEDIUM', true, NOW(), NOW()),
(25, 'Hyundai', 'Stargazer', 'LARGE', true, NOW(), NOW()),
(26, 'Kia', 'K3', 'MEDIUM', true, NOW(), NOW()),
(27, 'Kia', 'K5', 'MEDIUM', true, NOW(), NOW()),
(28, 'Kia', 'Seltos', 'MEDIUM', true, NOW(), NOW()),
(29, 'Kia', 'Sonet', 'SMALL', true, NOW(), NOW()),
(30, 'Kia', 'Sportage', 'MEDIUM', true, NOW(), NOW()),
(31, 'Kia', 'Carnival', 'LARGE', true, NOW(), NOW()),
(32, 'Kia', 'Sorento', 'LARGE', true, NOW(), NOW()),
(33, 'Mazda', '2', 'SMALL', true, NOW(), NOW()),
(34, 'Mazda', '6', 'MEDIUM', true, NOW(), NOW()),
(35, 'Mazda', 'CX-5', 'MEDIUM', true, NOW(), NOW()),
(36, 'Mazda', 'CX-8', 'LARGE', true, NOW(), NOW()),
(37, 'Mazda', 'BT-50', 'LARGE', true, NOW(), NOW()),
(38, 'Ford', 'Everest', 'LARGE', true, NOW(), NOW()),
(39, 'Ford', 'Explorer', 'LARGE', true, NOW(), NOW()),
(40, 'Ford', 'Territory', 'MEDIUM', true, NOW(), NOW()),
(41, 'Mitsubishi', 'Xpander', 'LARGE', true, NOW(), NOW()),
(42, 'Mitsubishi', 'Outlander', 'MEDIUM', true, NOW(), NOW()),
(43, 'Mitsubishi', 'Triton', 'LARGE', true, NOW(), NOW()),
(44, 'Mitsubishi', 'Attrage', 'SMALL', true, NOW(), NOW()),
(45, 'Mitsubishi', 'Pajero Sport', 'LARGE', true, NOW(), NOW()),
(46, 'Mitsubishi', 'Xforce', 'MEDIUM', true, NOW(), NOW()),
(47, 'VinFast', 'Fadil', 'SMALL', true, NOW(), NOW()),
(48, 'VinFast', 'VF 5', 'SMALL', true, NOW(), NOW()),
(49, 'VinFast', 'VF 6', 'MEDIUM', true, NOW(), NOW()),
(50, 'VinFast', 'VF 7', 'MEDIUM', true, NOW(), NOW()),
(51, 'VinFast', 'VF 8', 'LARGE', true, NOW(), NOW()),
(52, 'VinFast', 'VF 9', 'LARGE', true, NOW(), NOW()),
(53, 'VinFast', 'Lux A2.0', 'MEDIUM', true, NOW(), NOW()),
(54, 'VinFast', 'Lux SA2.0', 'LARGE', true, NOW(), NOW()),
(55, 'Suzuki', 'Swift', 'SMALL', true, NOW(), NOW()),
(56, 'Suzuki', 'Ertiga', 'LARGE', true, NOW(), NOW()),
(57, 'Suzuki', 'XL7', 'LARGE', true, NOW(), NOW()),
(58, 'Mercedes-Benz', 'C-Class', 'MEDIUM', true, NOW(), NOW()),
(59, 'Mercedes-Benz', 'E-Class', 'MEDIUM', true, NOW(), NOW()),
(60, 'Mercedes-Benz', 'S-Class', 'LARGE', true, NOW(), NOW()),
(61, 'Mercedes-Benz', 'GLC', 'MEDIUM', true, NOW(), NOW()),
(62, 'Mercedes-Benz', 'GLE', 'LARGE', true, NOW(), NOW()),
(63, 'BMW', '3 Series', 'MEDIUM', true, NOW(), NOW()),
(64, 'BMW', '5 Series', 'MEDIUM', true, NOW(), NOW()),
(65, 'BMW', 'X3', 'MEDIUM', true, NOW(), NOW()),
(66, 'BMW', 'X5', 'LARGE', true, NOW(), NOW());

-- 2.3. SERVICES
INSERT INTO services (id, name, description, is_main_service, is_active, created_at, updated_at) VALUES 
(1, 'Standard Wash', 'Rửa vỏ xe ngoài bằng xà phòng chuyên dụng và lau khô.', true, true, NOW(), NOW()),
(2, 'Premium Wash', 'Rửa vỏ xe chi tiết, dọn dẹp vệ sinh nội thất và hút bụi cabin.', true, true, NOW(), NOW()),
(3, 'Steam Engine Clean', 'Vệ sinh khoang máy ô tô bằng công nghệ hơi nước nóng.', false, true, NOW(), NOW()),
(4, 'Ceramic Coating', 'Phủ bóng ceramic bảo vệ lớp sơn bóng nhanh trong 60 phút.', false, true, NOW(), NOW());

-- 2.4. SERVICE_PRICES
INSERT INTO service_prices (id, service_id, vehicle_size, price, duration_minutes, is_active, created_at, updated_at) VALUES 
(1, 1, 'SMALL', 100000, 30, true, NOW(), NOW()),
(2, 1, 'MEDIUM', 130000, 35, true, NOW(), NOW()),
(3, 1, 'LARGE', 160000, 40, true, NOW(), NOW()),
(4, 2, 'SMALL', 180000, 45, true, NOW(), NOW()),
(5, 2, 'MEDIUM', 220000, 50, true, NOW(), NOW()),
(6, 2, 'LARGE', 260000, 55, true, NOW(), NOW()),
(7, 3, 'SMALL', 400000, 60, true, NOW(), NOW()),
(8, 3, 'MEDIUM', 450000, 70, true, NOW(), NOW()),
(9, 3, 'LARGE', 500000, 80, true, NOW(), NOW()),
(10, 4, 'SMALL', 600000, 90, true, NOW(), NOW()),
(11, 4, 'MEDIUM', 700000, 100, true, NOW(), NOW()),
(12, 4, 'LARGE', 800000, 110, true, NOW(), NOW());

-- 2.5. USERS
-- Hash for 'password123': $2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde
INSERT INTO users (id, name, email, phone, username, password, role, status, created_at, updated_at) VALUES 
(1, 'Admin AutoWash', 'admin@autowash.com', '0900000001', 'admin01', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'ADMIN', 'ACTIVE', NOW(), NOW()),
(2, 'Staff Nguyen Van A', 'staff01@autowash.com', '0900000002', 'staff01', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'STAFF', 'ACTIVE', NOW(), NOW()),
(3, 'Staff Tran Thi B', 'staff02@autowash.com', '0900000003', 'staff02', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'STAFF', 'ACTIVE', NOW(), NOW()),
(4, 'Customer Member', 'customer1@example.com', '0911111111', 'customer01', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'CUSTOMER', 'ACTIVE', NOW(), NOW()),
(5, 'Customer Silver', 'customer2@example.com', '0922222222', 'customer02', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'CUSTOMER', 'ACTIVE', NOW(), NOW()),
(6, 'Customer Gold', 'customer3@example.com', '0933333333', 'customer03', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'CUSTOMER', 'ACTIVE', NOW(), NOW()),
(7, 'Customer Platinum', 'customer4@example.com', '0944444444', 'customer04', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'CUSTOMER', 'ACTIVE', NOW(), NOW());

-- 2.6. CUSTOMER_PROFILES
INSERT INTO customer_profiles (user_id, tier_level, reward_points, tier_points, created_at, updated_at) VALUES 
(4, 'MEMBER', 50, 200, NOW(), NOW()),
(5, 'SILVER', 150, 1200, NOW(), NOW()),
(6, 'GOLD', 400, 3500, NOW(), NOW()),
(7, 'PLATINUM', 800, 7000, NOW(), NOW());

-- 2.7. CARS
INSERT INTO cars (id, user_id, license_plate, vehicle_model_id, status, created_at, updated_at) VALUES 
(1, 4, '30A-12345', 1, 'ACTIVE', NOW(), NOW()),
(2, 5, '30F-56789', 2, 'ACTIVE', NOW(), NOW()),
(3, 6, '51H-99999', 4, 'ACTIVE', NOW(), NOW()),
(4, 7, '29C-88888', 5, 'ACTIVE', NOW(), NOW());

-- 2.8. PROMOTIONS
INSERT INTO promotions (id, voucher_code, campaign_name, point_cost, target_tier, discount_amount, discount_percent, max_discount_amount, is_active, start_at, end_at, created_at, updated_at) VALUES 
(1, 'PROMO10', 'Grand Opening 10% Off', 50, 'MEMBER', NULL, 10.00, 50000, true, NOW() - INTERVAL '10 days', NOW() + INTERVAL '100 days', NOW(), NOW()),
(2, 'PROMOSILVER', 'Silver Tier Voucher 20k', 100, 'SILVER', 20000, NULL, NULL, true, NOW() - INTERVAL '10 days', NOW() + INTERVAL '100 days', NOW(), NOW()),
(3, 'PROMOGOLD', 'Gold Tier Voucher 50k', 200, 'GOLD', 50000, NULL, NULL, true, NOW() - INTERVAL '10 days', NOW() + INTERVAL '100 days', NOW(), NOW()),
(4, 'PROMOPLATINUM', 'Platinum VIP Voucher 100k', 300, 'PLATINUM', 100000, NULL, NULL, true, NOW() - INTERVAL '10 days', NOW() + INTERVAL '100 days', NOW(), NOW());

-- 2.9. CUSTOMER_VOUCHERS
INSERT INTO customer_vouchers (id, user_id, promotion_id, voucher_code, status, redeemed_at, used_at, expired_at) VALUES 
(1, 4, 1, 'PROMO10_001', 'AVAILABLE', NOW() - INTERVAL '1 day', NULL, NOW() + INTERVAL '30 days'),
(2, 5, 2, 'PROMOSILVER_001', 'USED', NOW() - INTERVAL '5 days', NOW() - INTERVAL '3 days', NOW() + INTERVAL '25 days'),
(3, 6, 3, 'PROMOGOLD_001', 'USED', NOW() - INTERVAL '4 days', NOW() - INTERVAL '2 days', NOW() + INTERVAL '26 days'),
(4, 7, 4, 'PROMOPLATINUM_001', 'AVAILABLE', NOW() - INTERVAL '1 day', NULL, NOW() + INTERVAL '29 days');

-- 2.10. BOOKINGS
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, total_price, qr_content, qr_used, arrived_at, wash_started_at, completed_at, staff_id, created_at, updated_at) VALUES 
(1, 'BK001', 4, 1, '2026-06-30 09:00:00', '2026-06-30 09:30:00', 'PENDING', NULL, false, 'Rửa sạch mâm xe giúp tôi.', 100000, 'AUTOWASH|BOOKING|BK001|d3b07384-d113-4a1b-a56e-821b017b2b73', false, NULL, NULL, NULL, NULL, NOW(), NOW()),
(2, 'BK002', 5, 2, '2026-06-30 10:00:00', '2026-06-30 10:50:00', 'PENDING', NULL, false, 'Hút bụi kỹ sàn xe.', 220000, 'AUTOWASH|BOOKING|BK002|f47ac10b-58cc-4372-a567-0e02b2c3d479', false, NULL, NULL, NULL, NULL, NOW(), NOW()),
(3, 'BK003', 6, 3, '2026-06-28 14:00:00', '2026-06-28 15:20:00', 'COMPLETED', 2, false, NULL, 500000, 'AUTOWASH|BOOKING|BK003|a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', true, '2026-06-28 13:55:00', '2026-06-28 14:00:00', '2026-06-28 15:20:00', 3, '2026-06-28 14:00:00', '2026-06-28 15:20:00'),
(4, 'BK004', 7, 4, '2026-06-27 16:00:00', '2026-06-27 16:55:00', 'COMPLETED', 1, false, NULL, 260000, 'AUTOWASH|BOOKING|BK004|9f8e7d6c-5b4a-3f2e-1d0c-9b8a7f6e5d4c', true, '2026-06-27 15:50:00', '2026-06-27 16:00:00', '2026-06-27 16:55:00', 2, '2026-06-27 16:00:00', '2026-06-27 16:55:00'),
(5, 'BK005', 6, 3, '2026-06-30 08:45:00', '2026-06-30 09:25:00', 'PENDING', NULL, false, 'Rửa kỹ mâm xe giùm em.', 160000, 'AUTOWASH|BOOKING|BK005|b8f6c5d4-a3e2-1b0c-9a8f-7e6d5c4b3a21', false, NULL, NULL, NULL, NULL, NOW(), NOW());

-- 2.11. BOOKING_DETAILS
INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes) VALUES 
(1, 1, 1, 100000, 30),
(2, 2, 5, 220000, 50),
(3, 3, 9, 500000, 80),
(4, 4, 6, 260000, 55),
(5, 5, 3, 160000, 40);

-- 2.12. PAYMENTS
INSERT INTO payments (id, booking_id, applied_voucher_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at) VALUES 
(1, 1, NULL, 'PENDING', 'PAYOS', 100000, 0, 100000, NULL, NOW(), NOW()),
(2, 2, NULL, 'PENDING', 'CASH', 220000, 0, 220000, NULL, NOW(), NOW()),
(3, 3, 3, 'PAID', 'CASH', 500000, 50000, 450000, '2026-06-28 15:20:00', '2026-06-28 14:00:00', '2026-06-28 15:20:00'),
(4, 4, NULL, 'PAID', 'PAYOS', 260000, 0, 260000, '2026-06-27 16:55:00', '2026-06-27 16:00:00', '2026-06-27 16:55:00'),
(5, 5, NULL, 'PENDING', 'PAYOS', 160000, 0, 160000, NULL, NOW(), NOW());

-- 2.13. REVIEWS
INSERT INTO reviews (id, booking_id, rating, comment, created_at) VALUES 
(1, 3, 5, 'Dịch vụ dọn khoang máy hơi nước siêu sạch, nhân viên tay nghề cao!', '2026-06-28 16:20:00'),
(2, 4, 4, 'Rửa xe rất kỹ, nhân viên niềm nở chu đáo. Sẽ quay lại thường xuyên.', '2026-06-27 18:55:00');

-- 2.14. OTP_TOKENS
INSERT INTO otp_tokens (id, user_id, email, otp_code, resend_count, is_verified, created_at, expired_at, purpose) VALUES 
(1, 4, 'customer1@example.com', '123456', 0, true, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days' + INTERVAL '1 hour', 'REGISTER'),
(2, 5, 'customer2@example.com', '654321', 1, false, NOW() - INTERVAL '15 minutes', NOW() + INTERVAL '15 minutes', 'REGISTER');

-- ------------------------------------------------------------------------
-- STEP 3: UPDATE IDENTITY SEQUENCES TO MATCH EXPLICIT IDs
-- ------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('vehicle_models', 'id'), coalesce(max(id), 1)) FROM vehicle_models;
SELECT setval(pg_get_serial_sequence('services', 'id'), coalesce(max(id), 1)) FROM services;
SELECT setval(pg_get_serial_sequence('service_prices', 'id'), coalesce(max(id), 1)) FROM service_prices;
SELECT setval(pg_get_serial_sequence('users', 'id'), coalesce(max(id), 1)) FROM users;
SELECT setval(pg_get_serial_sequence('cars', 'id'), coalesce(max(id), 1)) FROM cars;
SELECT setval(pg_get_serial_sequence('promotions', 'id'), coalesce(max(id), 1)) FROM promotions;
SELECT setval(pg_get_serial_sequence('customer_vouchers', 'id'), coalesce(max(id), 1)) FROM customer_vouchers;
SELECT setval(pg_get_serial_sequence('bookings', 'id'), coalesce(max(id), 1)) FROM bookings;
SELECT setval(pg_get_serial_sequence('booking_details', 'id'), coalesce(max(id), 1)) FROM booking_details;
SELECT setval(pg_get_serial_sequence('payments', 'id'), coalesce(max(id), 1)) FROM payments;
SELECT setval(pg_get_serial_sequence('reviews', 'id'), coalesce(max(id), 1)) FROM reviews;
SELECT setval(pg_get_serial_sequence('otp_tokens', 'id'), coalesce(max(id), 1)) FROM otp_tokens;

COMMIT;
