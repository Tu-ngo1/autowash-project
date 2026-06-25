-- ============================================================================
-- SQL SCRIPT: SEED DAILY OPERATIONS CONFIG AND SAMPLE VOUCHERS FOR AUTOWASH
-- Target Database: AutoWash (SQL Server)
-- ============================================================================

-- 1. Thêm cấu hình hoạt động hàng ngày (DAILY_OPERATIONS_CONFIG)
-- Tự động tạo cấu hình cho Hôm nay, Ngày mai và 8 ngày tiếp theo
-- Nếu đã tồn tại ngày tương ứng thì bỏ qua để tránh trùng khóa unique

PRINT 'Seeding DAILY_OPERATIONS_CONFIG...';

DECLARE @i INT = 0;
DECLARE @targetDate DATE;

WHILE @i <= 8
BEGIN
    SET @targetDate = CAST(DATEADD(day, @i, GETDATE()) AS DATE);
    
    IF NOT EXISTS (SELECT 1 FROM DAILY_OPERATIONS_CONFIG WHERE config_date = @targetDate)
    BEGIN
        INSERT INTO DAILY_OPERATIONS_CONFIG (config_date, open_time, close_time, bay_count, is_active)
        VALUES (@targetDate, '08:00:00', '18:00:00', 2, 1);
        PRINT 'Created config for ' + CAST(@targetDate AS VARCHAR(10));
    END
    ELSE
    BEGIN
        PRINT 'Config already exists for ' + CAST(@targetDate AS VARCHAR(10));
    END

    SET @i = @i + 1;
END;

-- 2. Thêm dữ liệu mẫu phân hạng (TIER_CONFIGS) nếu chưa có
PRINT 'Seeding TIER_CONFIGS...';
IF NOT EXISTS (SELECT 1 FROM TIER_CONFIGS WHERE tier_level = 'MEMBER')
    INSERT INTO TIER_CONFIGS (tier_level, is_active, auto_discount_percent, booking_window_days, points_to_maintain, points_to_upgrade)
    VALUES ('MEMBER', 1, 0.00, 7, 0, 1000);

IF NOT EXISTS (SELECT 1 FROM TIER_CONFIGS WHERE tier_level = 'SILVER')
    INSERT INTO TIER_CONFIGS (tier_level, is_active, auto_discount_percent, booking_window_days, points_to_maintain, points_to_upgrade)
    VALUES ('SILVER', 1, 2.00, 14, 1000, 3000);

IF NOT EXISTS (SELECT 1 FROM TIER_CONFIGS WHERE tier_level = 'GOLD')
    INSERT INTO TIER_CONFIGS (tier_level, is_active, auto_discount_percent, booking_window_days, points_to_maintain, points_to_upgrade)
    VALUES ('GOLD', 1, 5.00, 21, 3000, 6000);

IF NOT EXISTS (SELECT 1 FROM TIER_CONFIGS WHERE tier_level = 'PLATINUM')
    INSERT INTO TIER_CONFIGS (tier_level, is_active, auto_discount_percent, booking_window_days, points_to_maintain, points_to_upgrade)
    VALUES ('PLATINUM', 1, 10.00, 30, 6000, 999999);

-- 3. Tạo mẫu Promotions và gán Voucher cho các Customer mẫu
PRINT 'Checking promotions and customer vouchers...';
-- Lấy id của các khách hàng mẫu trong hệ thống
DECLARE @c1 BIGINT, @c2 BIGINT, @c3 BIGINT, @c4 BIGINT;
SELECT @c1 = id FROM USERS WHERE username = 'customer01';
SELECT @c2 = id FROM USERS WHERE username = 'customer02';
SELECT @c3 = id FROM USERS WHERE username = 'customer03';
SELECT @c4 = id FROM USERS WHERE username = 'customer04';

-- Nếu có khách hàng trong hệ thống, thực hiện gán các voucher mẫu
IF @c1 IS NOT NULL
BEGIN
    -- Đảm bảo có Promotion PROMO10
    DECLARE @p_promo10 BIGINT;
    SELECT @p_promo10 = id FROM PROMOTIONS WHERE voucher_code = 'PROMO10';
    
    IF @p_promo10 IS NULL
    BEGIN
        INSERT INTO PROMOTIONS (voucher_code, campaign_name, point_cost, target_tier, discount_amount, discount_percent, max_discount_amount, is_active, start_at, end_at, created_at, updated_at)
        VALUES ('PROMO10', 'Grand Opening 10% Off', 50, 'MEMBER', NULL, 10.00, 50000, 1, DATEADD(day, -10, GETDATE()), DATEADD(day, 100, GETDATE()), GETDATE(), GETDATE());
        SET @p_promo10 = SCOPE_IDENTITY();
    END

    -- Gán voucher PROMO10_001 cho customer01 nếu chưa có
    IF NOT EXISTS (SELECT 1 FROM CUSTOMER_VOUCHERS WHERE voucher_code = 'PROMO10_001')
    BEGIN
        INSERT INTO CUSTOMER_VOUCHERS (user_id, promotion_id, voucher_code, status, redeemed_at, used_at, expired_at)
        VALUES (@c1, @p_promo10, 'PROMO10_001', 'AVAILABLE', DATEADD(day, -1, GETDATE()), NULL, DATEADD(day, 30, GETDATE()));
        PRINT 'Assigned voucher PROMO10_001 to customer01';
    END
END

PRINT 'All seeding checks completed successfully!';
