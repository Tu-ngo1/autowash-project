-- ============================================================================
-- SQL SCRIPT: INSERT TEST DATA FOR EARLY ARRIVAL & MANUAL BAY ASSIGNMENT (PostgreSQL)
-- Description:
--   1. Booking 201: Xe đã Check-in (trạng thái ARRIVED), lịch hẹn 3 tiếng sau nhưng đến sớm hôm nay.
--      Staff có thể thấy xe trong "Hàng Đợi Điều Phối" và bấm "Chọn điều phối" -> "Vào khoang này".
--   2. Booking 202: Xe ở trạng thái CONFIRM, lịch hẹn 2 tiếng sau.
--      Dùng để test trọn vẹn luồng Check-in QR -> sang ARRIVED -> Đưa vào khoang rửa.
-- ============================================================================

-- Dọn dẹp dữ liệu cũ trùng ID 201, 202 nếu có
DELETE FROM reviews WHERE booking_id IN (201, 202);
DELETE FROM booking_details WHERE booking_id IN (201, 202);
DELETE FROM payments WHERE booking_id IN (201, 202);
DELETE FROM bookings WHERE id IN (201, 202);

-- ------------------------------------------------------------------------
-- 1. BOOKING ID 201: Đã ARRIVED (Đến sớm 3 tiếng, chưa gán khoang)
-- ------------------------------------------------------------------------
INSERT INTO bookings (
    id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, 
    status, total_price, qr_content, qr_used, is_late, arrived_at, created_at, updated_at, bay_number
) VALUES (
    201, 'BK-TEST-EARLY-ARRIVED', 4, 1, 
    NOW() + INTERVAL '3 hours', NOW() + INTERVAL '3 hours 45 minutes',
    'ARRIVED', 120000, 'AUTOWASH|BOOKING|BK-TEST-EARLY-ARRIVED|201', true, false, NOW(), NOW(), NOW(), NULL
);

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (201, 201, 1, 120000, 45);

INSERT INTO payments (
    id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at
) VALUES (
    201, 201, 'PAID', 'CASH', 120000, 0, 120000, NOW(), NOW(), NOW()
);

-- ------------------------------------------------------------------------
-- 2. BOOKING ID 202: Đặt lịch CONFIRM (Lịch 2 tiếng sau, chưa Check-in)
-- ------------------------------------------------------------------------
INSERT INTO bookings (
    id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, 
    status, total_price, qr_content, qr_used, is_late, created_at, updated_at, bay_number
) VALUES (
    202, 'BK-TEST-EARLY-CONFIRM', 5, 2, 
    NOW() + INTERVAL '2 hours', NOW() + INTERVAL '2 hours 45 minutes',
    'CONFIRM', 150000, 'AUTOWASH|BOOKING|BK-TEST-EARLY-CONFIRM|202', false, false, NOW(), NOW(), NOW(), NULL
);

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (202, 202, 2, 150000, 45);

INSERT INTO payments (
    id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at
) VALUES (
    202, 202, 'PAID', 'PAYOS', 150000, 0, 150000, NOW(), NOW(), NOW()
);
