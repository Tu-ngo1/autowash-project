-- ============================================================================
-- SQL SCRIPT: SEED DAILY OPERATIONS CONFIG AND SAMPLE VOUCHERS FOR AUTOWASH (PostgreSQL)
-- Target Database: AutoWash (PostgreSQL)
-- ============================================================================

DO $$
DECLARE
    i INT := 0;
    target_date DATE;
    c1 BIGINT;
    c2 BIGINT;
    c3 BIGINT;
    c4 BIGINT;
    p_promo10 BIGINT;
BEGIN
    -- 1. Thêm cấu hình hoạt động hàng ngày (DAILY_OPERATIONS_CONFIG)
    -- Tự động tạo cấu hình cho Hôm nay, Ngày mai và 8 ngày tiếp theo
    -- Nếu đã tồn tại ngày tương ứng thì bỏ qua để tránh trùng khóa unique
    RAISE NOTICE 'Seeding daily_operations_config...';
    
    WHILE i <= 8 LOOP
        target_date := (CURRENT_DATE + i * INTERVAL '1 day')::DATE;
        
        IF NOT EXISTS (SELECT 1 FROM daily_operations_config WHERE config_date = target_date) THEN
            INSERT INTO daily_operations_config (config_date, open_time, close_time, bay_count, is_active)
            VALUES (target_date, '08:00:00', '18:00:00', 2, true);
            RAISE NOTICE 'Created config for %', target_date;
        ELSE
            RAISE NOTICE 'Config already exists for %', target_date;
        END IF;
        
        i := i + 1;
    END LOOP;

    -- 2. Thêm dữ liệu mẫu phân hạng (TIER_CONFIGS) nếu chưa có
    RAISE NOTICE 'Seeding tier_configs...';
    
    IF NOT EXISTS (SELECT 1 FROM tier_configs WHERE tier_level = 'MEMBER') THEN
        INSERT INTO tier_configs (tier_level, is_active, auto_discount_percent, booking_window_days, points_to_maintain, points_to_upgrade)
        VALUES ('MEMBER', true, 0.00, 7, 0, 1000);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM tier_configs WHERE tier_level = 'SILVER') THEN
        INSERT INTO tier_configs (tier_level, is_active, auto_discount_percent, booking_window_days, points_to_maintain, points_to_upgrade)
        VALUES ('SILVER', true, 2.00, 14, 1000, 3000);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM tier_configs WHERE tier_level = 'GOLD') THEN
        INSERT INTO tier_configs (tier_level, is_active, auto_discount_percent, booking_window_days, points_to_maintain, points_to_upgrade)
        VALUES ('GOLD', true, 5.00, 21, 3000, 6000);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM tier_configs WHERE tier_level = 'PLATINUM') THEN
        INSERT INTO tier_configs (tier_level, is_active, auto_discount_percent, booking_window_days, points_to_maintain, points_to_upgrade)
        VALUES ('PLATINUM', true, 10.00, 30, 6000, 999999);
    END IF;

    -- 3. Tạo mẫu Promotions và gán Voucher cho các Customer mẫu
    RAISE NOTICE 'Checking promotions and customer vouchers...';
    
    -- Lấy id của các khách hàng mẫu trong hệ thống
    SELECT id INTO c1 FROM users WHERE username = 'customer01';
    SELECT id INTO c2 FROM users WHERE username = 'customer02';
    SELECT id INTO c3 FROM users WHERE username = 'customer03';
    SELECT id INTO c4 FROM users WHERE username = 'customer04';

    -- Nếu có khách hàng trong hệ thống, thực hiện gán các voucher mẫu
    IF c1 IS NOT NULL THEN
        -- Đảm bảo có Promotion PROMO10
        SELECT id INTO p_promo10 FROM promotions WHERE voucher_code = 'PROMO10';
        
        IF p_promo10 IS NULL THEN
            INSERT INTO promotions (voucher_code, campaign_name, point_cost, target_tier, discount_amount, discount_percent, max_discount_amount, is_active, start_at, end_at, created_at, updated_at)
            VALUES ('PROMO10', 'Grand Opening 10% Off', 50, 'MEMBER', NULL, 10.00, 50000, true, NOW() - INTERVAL '10 days', NOW() + INTERVAL '100 days', NOW(), NOW())
            RETURNING id INTO p_promo10;
        END IF;

        -- Gán voucher PROMO10_001 cho customer01 nếu chưa có
        IF NOT EXISTS (SELECT 1 FROM customer_vouchers WHERE voucher_code = 'PROMO10_001') THEN
            INSERT INTO customer_vouchers (user_id, promotion_id, voucher_code, status, redeemed_at, used_at, expired_at)
            VALUES (c1, p_promo10, 'PROMO10_001', 'AVAILABLE', NOW() - INTERVAL '1 day', NULL, NOW() + INTERVAL '30 days');
            RAISE NOTICE 'Assigned voucher PROMO10_001 to customer01';
        END IF;
    END IF;

    RAISE NOTICE 'All seeding checks completed successfully!';
END $$;
