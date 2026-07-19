-- ============================================================================
-- SQL SCRIPT: INSERT TEST DATA FOR CANCELLATION REQUESTS (PostgreSQL)
-- Description:
--   1. Ensures wallets exist for User 4 (Customer) and User 5 (Customer) to test refunds.
--   2. Inserts three bookings (IDs 101, 102, 103) representing PENDING, APPROVED, and REJECTED cancel requests.
-- ============================================================================

-- ------------------------------------------------------------------------
-- STEP 1: PREPARE WALLETS FOR USER 4 AND 5
-- ------------------------------------------------------------------------
INSERT INTO wallets (user_id, balance, updated_at)
VALUES 
(4, 200000, NOW()),
(5, 150000, NOW())
ON CONFLICT (user_id) DO UPDATE SET balance = EXCLUDED.balance;

-- ------------------------------------------------------------------------
-- STEP 2: INSERT CANCELLATION TEST DATA
-- ------------------------------------------------------------------------

-- 2.1. Booking ID 101: Cancel Request is PENDING (Chờ duyệt hủy)
-- Booking Status: CONFIRM. Staff 2 (Nguyen Van A) requested cancel.
-- Payment Status: PAID (PayOS). Customer 4.
INSERT INTO bookings (
    id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, 
    status, total_price, qr_content, qr_used, created_at, updated_at,
    cancel_request_status, cancel_request_reason, cancel_requested_by, cancel_requested_at
) VALUES (
    101, 'BK-CANCEL-PENDING', 4, 1, NOW() + INTERVAL '1 day', NOW() + INTERVAL '1 day 30 minutes',
    'CONFIRM', 100000, 'AUTOWASH|BOOKING|BK-CANCEL-PENDING|777f9999-55cc-4372-a567-0e02b2c3d999', false, NOW(), NOW(),
    'PENDING', 'Khoang máy rửa xe chuyên sâu đang bảo trì thiết bị áp lực đột xuất.', 2, NOW()
) ON CONFLICT (id) DO NOTHING;

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (101, 101, 1, 100000, 30)
ON CONFLICT (id) DO NOTHING;

INSERT INTO payments (
    id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at
) VALUES (
    101, 101, 'PAID', 'PAYOS', 100000, 0, 100000, NOW() - INTERVAL '1 hour', NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;


-- 2.2. Booking ID 102: Cancel Request is APPROVED (Đã duyệt hủy & Hoàn tiền)
-- Booking Status: CANCELLED. Staff 3 (Tran Thi B) requested.
-- Payment Status: REFUNDED (PayOS). Customer 5.
INSERT INTO bookings (
    id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, 
    status, total_price, qr_content, qr_used, created_at, updated_at,
    cancel_request_status, cancel_request_reason, cancel_requested_by, cancel_requested_at, cancel_request_admin_note
) VALUES (
    102, 'BK-CANCEL-APPROVED', 5, 2, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day 45 minutes',
    'CANCELLED', 180000, 'AUTOWASH|BOOKING|BK-CANCEL-APPROVED|888f9999-55cc-4372-a567-0e02b2c3d888', true, NOW() - INTERVAL '2 days', NOW(),
    'APPROVED', 'Thiếu nhân sự trực ca đêm do sự cố cá nhân đột xuất.', 3, NOW() - INTERVAL '1 day', 'Admin đã phê duyệt yêu cầu hủy và hệ thống tự động hoàn tiền cọc.'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (102, 102, 4, 180000, 45)
ON CONFLICT (id) DO NOTHING;

INSERT INTO payments (
    id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at
) VALUES (
    102, 102, 'REFUNDED', 'PAYOS', 180000, 0, 180000, NOW() - INTERVAL '1 day', NOW() - INTERVAL '2 days', NOW()
) ON CONFLICT (id) DO NOTHING;


-- 2.3. Booking ID 103: Cancel Request is REJECTED (Bị từ chối hủy)
-- Booking Status: CONFIRM (Quay lại luồng hoạt động bình thường). Staff 2 requested.
-- Payment Status: PAID (PayOS). Customer 4.
INSERT INTO bookings (
    id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, 
    status, total_price, qr_content, qr_used, created_at, updated_at,
    cancel_request_status, cancel_request_reason, cancel_requested_by, cancel_requested_at, cancel_request_admin_note
) VALUES (
    103, 'BK-CANCEL-REJECTED', 4, 1, NOW() + INTERVAL '2 days', NOW() + INTERVAL '2 days 30 minutes',
    'CONFIRM', 100000, 'AUTOWASH|BOOKING|BK-CANCEL-REJECTED|999f9999-55cc-4372-a567-0e02b2c3d777', false, NOW(), NOW(),
    'REJECTED', 'Thiết bị xịt bọt tuyết bị nghẹt van.', 2, NOW(), 'Cửa hàng đã bố trí thiết bị dự phòng thay thế. Tiếp tục phục vụ khách.'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (103, 103, 1, 100000, 30)
ON CONFLICT (id) DO NOTHING;

INSERT INTO payments (
    id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at
) VALUES (
    103, 103, 'PAID', 'PAYOS', 100000, 0, 100000, NOW() - INTERVAL '1 hour', NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;
