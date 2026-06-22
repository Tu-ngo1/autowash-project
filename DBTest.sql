-- ============================================================================
-- SQL SCRIPT: RE-CREATE AND SEED TEST DATA FOR AUTOWASH DATABASE
-- Description:
--   1. Safely deletes existing data in constraint-respecting order.
--   2. Resets auto-increment IDENTITY keys.
--   3. Inserts fresh, realistic test data for all 14 tables using SCOPE_IDENTITY()
--      variables to be completely independent of auto-increment start values.
-- Password for all seeded users: password123
-- ============================================================================

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;

-- START TRANSACTION
BEGIN TRANSACTION;
BEGIN TRY

    -- ------------------------------------------------------------------------
    -- STEP 1: DELETE OLD DATA (dependent-to-independent order)
    -- Adding WHERE clauses to prevent IDE warnings about delete without where
    -- ------------------------------------------------------------------------
    PRINT 'Deleting old data...';

    DELETE FROM REVIEWS WHERE id IS NOT NULL;
    DELETE FROM PAYMENTS WHERE id IS NOT NULL;
    DELETE FROM BOOKING_DETAILS WHERE id IS NOT NULL;
    DELETE FROM BOOKINGS WHERE id IS NOT NULL;
    DELETE FROM CUSTOMER_VOUCHERS WHERE id IS NOT NULL;
    DELETE FROM OTP_TOKENS WHERE id IS NOT NULL;
    DELETE FROM CUSTOMER_PROFILES WHERE id IS NOT NULL;
    DELETE FROM CARS WHERE id IS NOT NULL;
    DELETE FROM PROMOTIONS WHERE id IS NOT NULL;
    DELETE FROM SERVICE_PRICES WHERE id IS NOT NULL;
    DELETE FROM USERS WHERE id IS NOT NULL;
    DELETE FROM SERVICES WHERE id IS NOT NULL;
    DELETE FROM VEHICLE_MODELS WHERE id IS NOT NULL;
    DELETE FROM TIER_CONFIGS WHERE tier_level IS NOT NULL;

    -- ------------------------------------------------------------------------
    -- STEP 2: RESET IDENTITY COLUMNS (except tier_configs which has varchar PK)
    -- ------------------------------------------------------------------------
    PRINT 'Resetting identities...';

    DBCC CHECKIDENT ('REVIEWS', RESEED, 0);
    DBCC CHECKIDENT ('PAYMENTS', RESEED, 0);
    DBCC CHECKIDENT ('BOOKING_DETAILS', RESEED, 0);
    DBCC CHECKIDENT ('BOOKINGS', RESEED, 0);
    DBCC CHECKIDENT ('CUSTOMER_VOUCHERS', RESEED, 0);
    DBCC CHECKIDENT ('OTP_TOKENS', RESEED, 0);
    DBCC CHECKIDENT ('CUSTOMER_PROFILES', RESEED, 0);
    DBCC CHECKIDENT ('CARS', RESEED, 0);
    DBCC CHECKIDENT ('PROMOTIONS', RESEED, 0);
    DBCC CHECKIDENT ('SERVICE_PRICES', RESEED, 0);
    DBCC CHECKIDENT ('USERS', RESEED, 0);
    DBCC CHECKIDENT ('SERVICES', RESEED, 0);
    DBCC CHECKIDENT ('VEHICLE_MODELS', RESEED, 0);

    -- ------------------------------------------------------------------------
    -- STEP 3: DECLARE VARIABLES FOR GENERATED IDs
    -- ------------------------------------------------------------------------
    DECLARE @u_admin INT, @u_staff1 INT, @u_staff2 INT, @u_cust1 INT, @u_cust2 INT, @u_cust3 INT, @u_cust4 INT;
    DECLARE @vm_vios INT, @vm_civic INT, @vm_mazda3 INT, @vm_santafe INT, @vm_ranger INT, @vm_morning INT;
    DECLARE @s_std INT, @s_prem INT, @s_steam INT, @s_ceramic INT;
    DECLARE @sp_std_s INT, @sp_std_m INT, @sp_std_l INT;
    DECLARE @sp_prem_s INT, @sp_prem_m INT, @sp_prem_l INT;
    DECLARE @sp_steam_s INT, @sp_steam_m INT, @sp_steam_l INT;
    DECLARE @sp_ceramic_s INT, @sp_ceramic_m INT, @sp_ceramic_l INT;
    DECLARE @car_vios INT, @car_civic INT, @car_santafe INT, @car_ranger INT;
    DECLARE @p_promo10 INT, @p_silver INT, @p_gold INT, @p_platinum INT;
    DECLARE @cv_promo10 INT, @cv_silver INT, @cv_gold INT, @cv_platinum INT;
    DECLARE @bk1 INT, @bk2 INT, @bk3 INT, @bk4 INT;

    -- ------------------------------------------------------------------------
    -- STEP 4: INSERT FRESH SEED DATA
    -- ------------------------------------------------------------------------
    PRINT 'Inserting fresh seed data...';

    -- 4.1. TIER_CONFIGS (Varchar PK, no identity)
    INSERT INTO TIER_CONFIGS (tier_level, is_active, auto_discount_percent, booking_window_days, points_to_maintain, points_to_upgrade)
    VALUES 
    ('MEMBER', 1, 0.00, 7, 0, 1000),
    ('SILVER', 1, 2.00, 14, 1000, 3000),
    ('GOLD', 1, 5.00, 21, 3000, 6000),
    ('PLATINUM', 1, 10.00, 30, 6000, 999999);

    -- 4.2. VEHICLE_MODELS
    INSERT INTO VEHICLE_MODELS (brand, model_name, vehicle_size, is_active, created_at, updated_at) VALUES ('Toyota', 'Vios', 'SMALL', 1, GETDATE(), GETDATE());
    SET @vm_vios = SCOPE_IDENTITY();
    INSERT INTO VEHICLE_MODELS (brand, model_name, vehicle_size, is_active, created_at, updated_at) VALUES ('Honda', 'Civic', 'MEDIUM', 1, GETDATE(), GETDATE());
    SET @vm_civic = SCOPE_IDENTITY();
    INSERT INTO VEHICLE_MODELS (brand, model_name, vehicle_size, is_active, created_at, updated_at) VALUES ('Mazda', '3', 'MEDIUM', 1, GETDATE(), GETDATE());
    SET @vm_mazda3 = SCOPE_IDENTITY();
    INSERT INTO VEHICLE_MODELS (brand, model_name, vehicle_size, is_active, created_at, updated_at) VALUES ('Hyundai', 'SantaFe', 'LARGE', 1, GETDATE(), GETDATE());
    SET @vm_santafe = SCOPE_IDENTITY();
    INSERT INTO VEHICLE_MODELS (brand, model_name, vehicle_size, is_active, created_at, updated_at) VALUES ('Ford', 'Ranger', 'LARGE', 1, GETDATE(), GETDATE());
    SET @vm_ranger = SCOPE_IDENTITY();
    INSERT INTO VEHICLE_MODELS (brand, model_name, vehicle_size, is_active, created_at, updated_at) VALUES ('Kia', 'Morning', 'SMALL', 1, GETDATE(), GETDATE());
    SET @vm_morning = SCOPE_IDENTITY();

    -- 4.3. SERVICES
    INSERT INTO SERVICES (name, description, is_main_service, is_active, created_at, updated_at) VALUES ('Standard Wash', N'Rửa vỏ xe ngoài bằng xà phòng chuyên dụng và lau khô.', 1, 1, GETDATE(), GETDATE());
    SET @s_std = SCOPE_IDENTITY();
    INSERT INTO SERVICES (name, description, is_main_service, is_active, created_at, updated_at) VALUES ('Premium Wash', N'Rửa vỏ xe chi tiết, dọn dẹp vệ sinh nội thất và hút bụi cabin.', 1, 1, GETDATE(), GETDATE());
    SET @s_prem = SCOPE_IDENTITY();
    INSERT INTO SERVICES (name, description, is_main_service, is_active, created_at, updated_at) VALUES ('Steam Engine Clean', N'Vệ sinh khoang máy ô tô bằng công nghệ hơi nước nóng.', 0, 1, GETDATE(), GETDATE());
    SET @s_steam = SCOPE_IDENTITY();
    INSERT INTO SERVICES (name, description, is_main_service, is_active, created_at, updated_at) VALUES ('Ceramic Coating', N'Phủ bóng ceramic bảo vệ lớp sơn bóng nhanh trong 60 phút.', 0, 1, GETDATE(), GETDATE());
    SET @s_ceramic = SCOPE_IDENTITY();

    -- 4.4. SERVICE_PRICES
    -- Service 1 (Standard Wash)
    INSERT INTO SERVICE_PRICES (service_id, vehicle_size, price, duration_minutes, is_active, created_at, updated_at) VALUES (@s_std, 'SMALL', 100000, 30, 1, GETDATE(), GETDATE());
    SET @sp_std_s = SCOPE_IDENTITY();
    INSERT INTO SERVICE_PRICES (service_id, vehicle_size, price, duration_minutes, is_active, created_at, updated_at) VALUES (@s_std, 'MEDIUM', 130000, 35, 1, GETDATE(), GETDATE());
    SET @sp_std_m = SCOPE_IDENTITY();
    INSERT INTO SERVICE_PRICES (service_id, vehicle_size, price, duration_minutes, is_active, created_at, updated_at) VALUES (@s_std, 'LARGE', 160000, 40, 1, GETDATE(), GETDATE());
    SET @sp_std_l = SCOPE_IDENTITY();

    -- Service 2 (Premium Wash)
    INSERT INTO SERVICE_PRICES (service_id, vehicle_size, price, duration_minutes, is_active, created_at, updated_at) VALUES (@s_prem, 'SMALL', 180000, 45, 1, GETDATE(), GETDATE());
    SET @sp_prem_s = SCOPE_IDENTITY();
    INSERT INTO SERVICE_PRICES (service_id, vehicle_size, price, duration_minutes, is_active, created_at, updated_at) VALUES (@s_prem, 'MEDIUM', 220000, 50, 1, GETDATE(), GETDATE());
    SET @sp_prem_m = SCOPE_IDENTITY();
    INSERT INTO SERVICE_PRICES (service_id, vehicle_size, price, duration_minutes, is_active, created_at, updated_at) VALUES (@s_prem, 'LARGE', 260000, 55, 1, GETDATE(), GETDATE());
    SET @sp_prem_l = SCOPE_IDENTITY();

    -- Service 3 (Steam Engine Clean)
    INSERT INTO SERVICE_PRICES (service_id, vehicle_size, price, duration_minutes, is_active, created_at, updated_at) VALUES (@s_steam, 'SMALL', 400000, 60, 1, GETDATE(), GETDATE());
    SET @sp_steam_s = SCOPE_IDENTITY();
    INSERT INTO SERVICE_PRICES (service_id, vehicle_size, price, duration_minutes, is_active, created_at, updated_at) VALUES (@s_steam, 'MEDIUM', 450000, 70, 1, GETDATE(), GETDATE());
    SET @sp_steam_m = SCOPE_IDENTITY();
    INSERT INTO SERVICE_PRICES (service_id, vehicle_size, price, duration_minutes, is_active, created_at, updated_at) VALUES (@s_steam, 'LARGE', 500000, 80, 1, GETDATE(), GETDATE());
    SET @sp_steam_l = SCOPE_IDENTITY();

    -- Service 4 (Ceramic Coating)
    INSERT INTO SERVICE_PRICES (service_id, vehicle_size, price, duration_minutes, is_active, created_at, updated_at) VALUES (@s_ceramic, 'SMALL', 600000, 90, 1, GETDATE(), GETDATE());
    SET @sp_ceramic_s = SCOPE_IDENTITY();
    INSERT INTO SERVICE_PRICES (service_id, vehicle_size, price, duration_minutes, is_active, created_at, updated_at) VALUES (@s_ceramic, 'MEDIUM', 700000, 100, 1, GETDATE(), GETDATE());
    SET @sp_ceramic_m = SCOPE_IDENTITY();
    INSERT INTO SERVICE_PRICES (service_id, vehicle_size, price, duration_minutes, is_active, created_at, updated_at) VALUES (@s_ceramic, 'LARGE', 800000, 110, 1, GETDATE(), GETDATE());
    SET @sp_ceramic_l = SCOPE_IDENTITY();

    -- 4.5. USERS
    -- Hash for 'password123': $2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde
    INSERT INTO USERS (name, email, phone, username, password, role, status, created_at, updated_at) VALUES ('Admin AutoWash', 'admin@autowash.com', '0900000001', 'admin01', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'ADMIN', 'ACTIVE', GETDATE(), GETDATE());
    SET @u_admin = SCOPE_IDENTITY();
    INSERT INTO USERS (name, email, phone, username, password, role, status, created_at, updated_at) VALUES ('Staff Nguyen Van A', 'staff01@autowash.com', '0900000002', 'staff01', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'STAFF', 'ACTIVE', GETDATE(), GETDATE());
    SET @u_staff1 = SCOPE_IDENTITY();
    INSERT INTO USERS (name, email, phone, username, password, role, status, created_at, updated_at) VALUES ('Staff Tran Thi B', 'staff02@autowash.com', '0900000003', 'staff02', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'STAFF', 'ACTIVE', GETDATE(), GETDATE());
    SET @u_staff2 = SCOPE_IDENTITY();
    INSERT INTO USERS (name, email, phone, username, password, role, status, created_at, updated_at) VALUES ('Customer Member', 'customer1@example.com', '0911111111', 'customer01', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'CUSTOMER', 'ACTIVE', GETDATE(), GETDATE());
    SET @u_cust1 = SCOPE_IDENTITY();
    INSERT INTO USERS (name, email, phone, username, password, role, status, created_at, updated_at) VALUES ('Customer Silver', 'customer2@example.com', '0922222222', 'customer02', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'CUSTOMER', 'ACTIVE', GETDATE(), GETDATE());
    SET @u_cust2 = SCOPE_IDENTITY();
    INSERT INTO USERS (name, email, phone, username, password, role, status, created_at, updated_at) VALUES ('Customer Gold', 'customer3@example.com', '0933333333', 'customer03', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'CUSTOMER', 'ACTIVE', GETDATE(), GETDATE());
    SET @u_cust3 = SCOPE_IDENTITY();
    INSERT INTO USERS (name, email, phone, username, password, role, status, created_at, updated_at) VALUES ('Customer Platinum', 'customer4@example.com', '0944444444', 'customer04', '$2a$10$WnYzD3yFeA1JfRK6vodMseSlnDJEbEUe.DJdaJ7xkbHnsj0fWBxde', 'CUSTOMER', 'ACTIVE', GETDATE(), GETDATE());
    SET @u_cust4 = SCOPE_IDENTITY();

    -- 4.6. CUSTOMER_PROFILES
    INSERT INTO CUSTOMER_PROFILES (user_id, tier_level, reward_points, tier_points, created_at, updated_at) VALUES (@u_cust1, 'MEMBER', 50, 200, GETDATE(), GETDATE());
    INSERT INTO CUSTOMER_PROFILES (user_id, tier_level, reward_points, tier_points, created_at, updated_at) VALUES (@u_cust2, 'SILVER', 150, 1200, GETDATE(), GETDATE());
    INSERT INTO CUSTOMER_PROFILES (user_id, tier_level, reward_points, tier_points, created_at, updated_at) VALUES (@u_cust3, 'GOLD', 400, 3500, GETDATE(), GETDATE());
    INSERT INTO CUSTOMER_PROFILES (user_id, tier_level, reward_points, tier_points, created_at, updated_at) VALUES (@u_cust4, 'PLATINUM', 800, 7000, GETDATE(), GETDATE());

    -- 4.7. CARS (Bỏ cột vehicle_size)
    INSERT INTO CARS (user_id, license_plate, vehicle_model_id, Status, created_at, updated_at) VALUES (@u_cust1, '30A-12345', @vm_vios, 'ACTIVE', GETDATE(), GETDATE());
    SET @car_vios = SCOPE_IDENTITY();
    INSERT INTO CARS (user_id, license_plate, vehicle_model_id, Status, created_at, updated_at) VALUES (@u_cust2, '30F-56789', @vm_civic, 'ACTIVE', GETDATE(), GETDATE());
    SET @car_civic = SCOPE_IDENTITY();
    INSERT INTO CARS (user_id, license_plate, vehicle_model_id, Status, created_at, updated_at) VALUES (@u_cust3, '51H-99999', @vm_santafe, 'ACTIVE', GETDATE(), GETDATE());
    SET @car_santafe = SCOPE_IDENTITY();
    INSERT INTO CARS (user_id, license_plate, vehicle_model_id, Status, created_at, updated_at) VALUES (@u_cust4, '29C-88888', @vm_ranger, 'ACTIVE', GETDATE(), GETDATE());
    SET @car_ranger = SCOPE_IDENTITY();

    -- 4.8. PROMOTIONS
    INSERT INTO PROMOTIONS (voucher_code, campaign_name, point_cost, target_tier, discount_amount, discount_percent, max_discount_amount, is_active, start_at, end_at, created_at, updated_at)
    VALUES ('PROMO10', 'Grand Opening 10% Off', 50, 'MEMBER', NULL, 10.00, 50000, 1, DATEADD(day, -10, GETDATE()), DATEADD(day, 100, GETDATE()), GETDATE(), GETDATE());
    SET @p_promo10 = SCOPE_IDENTITY();
    INSERT INTO PROMOTIONS (voucher_code, campaign_name, point_cost, target_tier, discount_amount, discount_percent, max_discount_amount, is_active, start_at, end_at, created_at, updated_at)
    VALUES ('PROMOSILVER', 'Silver Tier Voucher 20k', 100, 'SILVER', 20000, NULL, NULL, 1, DATEADD(day, -10, GETDATE()), DATEADD(day, 100, GETDATE()), GETDATE(), GETDATE());
    SET @p_silver = SCOPE_IDENTITY();
    INSERT INTO PROMOTIONS (voucher_code, campaign_name, point_cost, target_tier, discount_amount, discount_percent, max_discount_amount, is_active, start_at, end_at, created_at, updated_at)
    VALUES ('PROMOGOLD', 'Gold Tier Voucher 50k', 200, 'GOLD', 50000, NULL, NULL, 1, DATEADD(day, -10, GETDATE()), DATEADD(day, 100, GETDATE()), GETDATE(), GETDATE());
    SET @p_gold = SCOPE_IDENTITY();
    INSERT INTO PROMOTIONS (voucher_code, campaign_name, point_cost, target_tier, discount_amount, discount_percent, max_discount_amount, is_active, start_at, end_at, created_at, updated_at)
    VALUES ('PROMOPLATINUM', 'Platinum VIP Voucher 100k', 300, 'PLATINUM', 100000, NULL, NULL, 1, DATEADD(day, -10, GETDATE()), DATEADD(day, 100, GETDATE()), GETDATE(), GETDATE());
    SET @p_platinum = SCOPE_IDENTITY();

    -- 4.9. CUSTOMER_VOUCHERS
    INSERT INTO CUSTOMER_VOUCHERS (user_id, promotion_id, voucher_code, status, redeemed_at, used_at, expired_at) VALUES (@u_cust1, @p_promo10, 'PROMO10_001', 'AVAILABLE', DATEADD(day, -1, GETDATE()), NULL, DATEADD(day, 30, GETDATE()));
    SET @cv_promo10 = SCOPE_IDENTITY();
    INSERT INTO CUSTOMER_VOUCHERS (user_id, promotion_id, voucher_code, status, redeemed_at, used_at, expired_at) VALUES (@u_cust2, @p_silver, 'PROMOSILVER_001', 'USED', DATEADD(day, -5, GETDATE()), DATEADD(day, -3, GETDATE()), DATEADD(day, 25, GETDATE()));
    SET @cv_silver = SCOPE_IDENTITY();
    INSERT INTO CUSTOMER_VOUCHERS (user_id, promotion_id, voucher_code, status, redeemed_at, used_at, expired_at) VALUES (@u_cust3, @p_gold, 'PROMOGOLD_001', 'USED', DATEADD(day, -4, GETDATE()), DATEADD(day, -2, GETDATE()), DATEADD(day, 26, GETDATE()));
    SET @cv_gold = SCOPE_IDENTITY();
    INSERT INTO CUSTOMER_VOUCHERS (user_id, promotion_id, voucher_code, status, redeemed_at, used_at, expired_at) VALUES (@u_cust4, @p_platinum, 'PROMOPLATINUM_001', 'AVAILABLE', DATEADD(day, -1, GETDATE()), NULL, DATEADD(day, 29, GETDATE()));
    SET @cv_platinum = SCOPE_IDENTITY();

    -- 4.10. BOOKINGS
    INSERT INTO BOOKINGS (booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, Total_price, Qr_content, Qr_used, arrived_at, wash_started_at, completed_at, created_at, updated_at)
    VALUES ('BK001', @u_cust1, @car_vios, DATEADD(day, 2, GETDATE()), DATEADD(minute, 30, DATEADD(day, 2, GETDATE())), 'PENDING', NULL, 0, N'Rửa sạch mâm xe giúp tôi.', 100000, 'BK001_QR_CODE', 0, NULL, NULL, NULL, GETDATE(), GETDATE());
    SET @bk1 = SCOPE_IDENTITY();
    INSERT INTO BOOKINGS (booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, Total_price, Qr_content, Qr_used, arrived_at, wash_started_at, completed_at, created_at, updated_at)
    VALUES ('BK002', @u_cust2, @car_civic, DATEADD(day, 1, GETDATE()), DATEADD(minute, 50, DATEADD(day, 1, GETDATE())), 'ARRIVED', 1, 0, N'Hút bụi kỹ sàn xe.', 220000, 'BK002_QR_CODE', 0, NULL, NULL, NULL, GETDATE(), GETDATE());
    SET @bk2 = SCOPE_IDENTITY();
    INSERT INTO BOOKINGS (booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, Total_price, Qr_content, Qr_used, arrived_at, wash_started_at, completed_at, created_at, updated_at)
    VALUES ('BK003', @u_cust3, @car_santafe, DATEADD(day, -2, GETDATE()), DATEADD(minute, 80, DATEADD(day, -2, GETDATE())), 'COMPLETED', 2, 0, NULL, 500000, 'BK003_QR_CODE', 1, DATEADD(minute, -5, DATEADD(day, -2, GETDATE())), DATEADD(day, -2, GETDATE()), DATEADD(minute, 80, DATEADD(day, -2, GETDATE())), DATEADD(day, -2, GETDATE()), DATEADD(day, -2, GETDATE()));
    SET @bk3 = SCOPE_IDENTITY();
    INSERT INTO BOOKINGS (booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, Total_price, Qr_content, Qr_used, arrived_at, wash_started_at, completed_at, created_at, updated_at)
    VALUES ('BK004', @u_cust4, @car_ranger, DATEADD(day, -3, GETDATE()), DATEADD(minute, 55, DATEADD(day, -3, GETDATE())), 'COMPLETED', 1, 0, NULL, 260000, 'BK004_QR_CODE', 1, DATEADD(minute, -10, DATEADD(day, -3, GETDATE())), DATEADD(day, -3, GETDATE()), DATEADD(minute, 55, DATEADD(day, -3, GETDATE())), DATEADD(day, -3, GETDATE()), DATEADD(day, -3, GETDATE()));
    SET @bk4 = SCOPE_IDENTITY();

    -- 4.11. BOOKING_DETAILS (Bỏ cột service_id)
    INSERT INTO BOOKING_DETAILS (booking_id, service_price_id, actual_price, actual_duration_minutes) VALUES (@bk1, @sp_std_s, 100000, 30);
    INSERT INTO BOOKING_DETAILS (booking_id, service_price_id, actual_price, actual_duration_minutes) VALUES (@bk2, @sp_prem_m, 220000, 50);
    INSERT INTO BOOKING_DETAILS (booking_id, service_price_id, actual_price, actual_duration_minutes) VALUES (@bk3, @sp_steam_l, 500000, 80);
    INSERT INTO BOOKING_DETAILS (booking_id, service_price_id, actual_price, actual_duration_minutes) VALUES (@bk4, @sp_prem_l, 260000, 55);

    -- 4.12. PAYMENTS
    INSERT INTO PAYMENTS (booking_id, applied_voucher_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at) VALUES (@bk1, NULL, 'PENDING', 'PAYOS', 100000, 0, 100000, NULL, GETDATE(), GETDATE());
    INSERT INTO PAYMENTS (booking_id, applied_voucher_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at) VALUES (@bk2, NULL, 'PENDING', 'CASH', 220000, 0, 220000, NULL, GETDATE(), GETDATE());
    INSERT INTO PAYMENTS (booking_id, applied_voucher_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at) VALUES (@bk3, @cv_gold, 'PAID', 'CASH', 500000, 50000, 450000, DATEADD(minute, 80, DATEADD(day, -2, GETDATE())), DATEADD(day, -2, GETDATE()), DATEADD(day, -2, GETDATE()));
    INSERT INTO PAYMENTS (booking_id, applied_voucher_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at) VALUES (@bk4, NULL, 'PAID', 'PAYOS', 260000, 0, 260000, DATEADD(minute, 55, DATEADD(day, -3, GETDATE())), DATEADD(day, -3, GETDATE()), DATEADD(day, -3, GETDATE()));

    -- 4.13. REVIEWS
    INSERT INTO REVIEWS (booking_id, rating, comment, created_at) VALUES (@bk3, 5, N'Dịch vụ dọn khoang máy hơi nước siêu sạch, nhân viên tay nghề cao!', DATEADD(hour, 1, DATEADD(minute, 80, DATEADD(day, -2, GETDATE()))));
    INSERT INTO REVIEWS (booking_id, rating, comment, created_at) VALUES (@bk4, 4, N'Rửa xe rất kỹ, nhân viên niềm nở chu đáo. Sẽ quay lại thường xuyên.', DATEADD(hour, 2, DATEADD(minute, 55, DATEADD(day, -3, GETDATE()))));

    -- 4.14. OTP_TOKENS
    INSERT INTO OTP_TOKENS (user_id, email, otp_code, resend_count, is_verified, created_at, expired_at, purpose) VALUES (@u_cust1, 'customer1@example.com', '123456', 0, 1, DATEADD(day, -10, GETDATE()), DATEADD(hour, 1, DATEADD(day, -10, GETDATE())), 'REGISTER');
    INSERT INTO OTP_TOKENS (user_id, email, otp_code, resend_count, is_verified, created_at, expired_at, purpose) VALUES (@u_cust2, 'customer2@example.com', '654321', 1, 0, DATEADD(minute, -15, GETDATE()), DATEADD(minute, 15, GETDATE()), 'REGISTER');

    -- COMMIT TRANSACTION IF EVERYTHING OK
    COMMIT TRANSACTION;
    PRINT 'Database re-created and seeded successfully!';

END TRY
BEGIN CATCH
    -- ROLLBACK ON ERROR
    ROLLBACK TRANSACTION;
    PRINT 'Error occurred during transaction!';
    THROW;
END CATCH;