-- ============================================================================
-- SQL SCRIPT: SEED MASSIVE REALISTIC TEST DATA FOR AUTOWASH DATABASE (PostgreSQL)
-- Description:
--   Chèn một lượng lớn dữ liệu đặt lịch (lên tới 25+ đơn đặt lịch chân thực)
--   bao phủ đầy đủ các trạng thái, ngày giờ, doanh thu để biểu diễn Dashboard
--   và kiểm thử nghiệp vụ một cách hoàn hảo nhất trước Hội đồng.
--   Phân bổ:
--     - Lịch sử tuần trước (Đã hoàn thành - COMPLETED)
--     - Lịch hẹn hôm nay (Đủ mọi trạng thái: COMPLETED, IN_PROGRESS, ARRIVED, CONFIRM)
--     - Lịch hẹn tương lai (CONFIRM, PENDING, Chờ duyệt hủy PENDING, Đã hủy CANCELLED)
-- ============================================================================

BEGIN;

-- ------------------------------------------------------------------------
-- BƯỚC 1: CHUẨN BỊ VÍ & SỐ DƯ CHO KHÁCH HÀNG (WALLET SEED)
-- ------------------------------------------------------------------------
INSERT INTO wallets (user_id, balance, updated_at)
VALUES 
(4, 500000, NOW()), -- Customer Member: 500,000đ
(5, 750000, NOW()), -- Customer Silver: 750,000đ
(6, 1200000, NOW()), -- Customer Gold: 1,200,000đ
(7, 2000000, NOW())  -- Customer Platinum: 2,000,000đ
ON CONFLICT (user_id) DO UPDATE SET balance = EXCLUDED.balance;

-- ------------------------------------------------------------------------
-- BƯỚC 2: XÓA DỮ LIỆU ĐỂ TRÁNH TRÙNG LẶP CHO CÁC ID DƯỚI ĐÂY (TỪ ID 200 TRỞ LÊN)
-- ------------------------------------------------------------------------
DELETE FROM reviews WHERE booking_id >= 200;
DELETE FROM payments WHERE booking_id >= 200;
DELETE FROM booking_details WHERE booking_id >= 200;
DELETE FROM bookings WHERE id >= 200;

-- ------------------------------------------------------------------------
-- BƯỚC 3: CHÈN DỮ LIỆU LỊCH SỬ ĐẶT LỊCH (QUÁ KHỨ - TUẦN TRƯỚC)
-- Trạng thái: COMPLETED. Thanh toán: PAID.
-- ------------------------------------------------------------------------

-- Đơn 200: Khách 4, Rửa Standard Wash (100k), Thanh toán Tiền mặt (CASH)
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, total_price, qr_content, qr_used, arrived_at, wash_started_at, completed_at, staff_id, created_at, updated_at)
VALUES (200, 'BK200', 4, 1, NOW() - INTERVAL '7 days 4 hours', NOW() - INTERVAL '7 days 3.5 hours', 'COMPLETED', 1, false, NULL, 100000, 'AUTOWASH|BOOKING|BK200|uuid-200', true, NOW() - INTERVAL '7 days 4.1 hours', NOW() - INTERVAL '7 days 4.0 hours', NOW() - INTERVAL '7 days 3.5 hours', 2, NOW() - INTERVAL '7 days 5 hours', NOW() - INTERVAL '7 days 3.5 hours');

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (200, 200, 1, 100000, 30);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (200, 200, 'PAID', 'CASH', 100000, 0, 100000, NOW() - INTERVAL '7 days 3.5 hours', NOW() - INTERVAL '7 days 5 hours', NOW() - INTERVAL '7 days 3.5 hours');

-- Đơn 201: Khách 5, Rửa Premium Wash (220k), Thanh toán PayOS
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, total_price, qr_content, qr_used, arrived_at, wash_started_at, completed_at, staff_id, created_at, updated_at)
VALUES (201, 'BK201', 5, 2, NOW() - INTERVAL '6 days 3 hours', NOW() - INTERVAL '6 days 2.1 hours', 'COMPLETED', 2, false, NULL, 220000, 'AUTOWASH|BOOKING|BK201|uuid-201', true, NOW() - INTERVAL '6 days 3.2 hours', NOW() - INTERVAL '6 days 3.0 hours', NOW() - INTERVAL '6 days 2.1 hours', 3, NOW() - INTERVAL '6 days 4 hours', NOW() - INTERVAL '6 days 2.1 hours');

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (201, 201, 5, 220000, 50);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (201, 201, 'PAID', 'PAYOS', 220000, 4400, 215600, NOW() - INTERVAL '6 days 3.2 hours', NOW() - INTERVAL '6 days 4 hours', NOW() - INTERVAL '6 days 2.1 hours');

-- Đơn 202: Khách 6, Rửa Ceramic Coating (700k), Thanh toán Ví (WALLET)
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, total_price, qr_content, qr_used, arrived_at, wash_started_at, completed_at, staff_id, created_at, updated_at)
VALUES (202, 'BK202', 6, 3, NOW() - INTERVAL '5 days 2 hours', NOW() - INTERVAL '5 days 0.3 hours', 'COMPLETED', 1, false, 'Đánh bóng kỹ sơn xe', 700000, 'AUTOWASH|BOOKING|BK202|uuid-202', true, NOW() - INTERVAL '5 days 2.1 hours', NOW() - INTERVAL '5 days 2.0 hours', NOW() - INTERVAL '5 days 0.3 hours', 2, NOW() - INTERVAL '5 days 3 hours', NOW() - INTERVAL '5 days 0.3 hours');

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (202, 202, 11, 700000, 100);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (202, 202, 'PAID', 'WALLET', 700000, 35000, 665000, NOW() - INTERVAL '5 days 3 hours', NOW() - INTERVAL '5 days 3 hours', NOW() - INTERVAL '5 days 0.3 hours');

-- Đơn 203: Khách 7, Rửa Premium Wash (260k) + Engine Clean (500k) = 760k. Hạng Platinum giảm 10% -> 684k.
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, total_price, qr_content, qr_used, arrived_at, wash_started_at, completed_at, staff_id, created_at, updated_at)
VALUES (203, 'BK203', 7, 4, NOW() - INTERVAL '4 days 5 hours', NOW() - INTERVAL '4 days 3.5 hours', 'COMPLETED', 2, false, NULL, 760000, 'AUTOWASH|BOOKING|BK203|uuid-203', true, NOW() - INTERVAL '4 days 5.0 hours', NOW() - INTERVAL '4 days 4.8 hours', NOW() - INTERVAL '4 days 3.5 hours', 3, NOW() - INTERVAL '4 days 6 hours', NOW() - INTERVAL '4 days 3.5 hours');

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES 
(203, 203, 6, 260000, 55),
(204, 203, 9, 500000, 80);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (203, 203, 'PAID', 'PAYOS', 760000, 76000, 684000, NOW() - INTERVAL '4 days 6 hours', NOW() - INTERVAL '4 days 6 hours', NOW() - INTERVAL '4 days 3.5 hours');

-- Đơn 204: Khách 4, Rửa Standard Wash (100k) - Đã hoàn thành cách đây 3 ngày.
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, total_price, qr_content, qr_used, arrived_at, wash_started_at, completed_at, staff_id, created_at, updated_at)
VALUES (205, 'BK205', 4, 1, NOW() - INTERVAL '3 days 6 hours', NOW() - INTERVAL '3 days 5.5 hours', 'COMPLETED', 1, false, NULL, 100000, 'AUTOWASH|BOOKING|BK205|uuid-205', true, NOW() - INTERVAL '3 days 6.1 hours', NOW() - INTERVAL '3 days 6.0 hours', NOW() - INTERVAL '3 days 5.5 hours', 2, NOW() - INTERVAL '3 days 7 hours', NOW() - INTERVAL '3 days 5.5 hours');

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (205, 205, 1, 100000, 30);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (205, 205, 'PAID', 'CASH', 100000, 0, 100000, NOW() - INTERVAL '3 days 5.5 hours', NOW() - INTERVAL '3 days 7 hours', NOW() - INTERVAL '3 days 5.5 hours');

-- Thêm Review cho đơn 205
INSERT INTO reviews (id, booking_id, rating, comment, created_at)
VALUES (200, 205, 5, 'Dịch vụ nhanh gọn sạch sẽ, nhân viên thân thiện!', NOW() - INTERVAL '3 days 5 hours');


-- ------------------------------------------------------------------------
-- BƯỚC 4: CHÈN DỮ LIỆU ĐƠN ĐẶT LỊCH HÔM NAY (TODAY BOOKINGS)
-- ------------------------------------------------------------------------

-- Đơn 206: Đã hoàn thành sáng nay (COMPLETED)
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, total_price, qr_content, qr_used, arrived_at, wash_started_at, completed_at, staff_id, created_at, updated_at)
VALUES (206, 'BK206', 5, 2, NOW() - INTERVAL '5 hours', NOW() - INTERVAL '4.2 hours', 'COMPLETED', 1, false, NULL, 220000, 'AUTOWASH|BOOKING|BK206|uuid-206', true, NOW() - INTERVAL '5.1 hours', NOW() - INTERVAL '5.0 hours', NOW() - INTERVAL '4.2 hours', 2, NOW() - INTERVAL '6 hours', NOW() - INTERVAL '4.2 hours');

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (206, 206, 5, 220000, 50);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (206, 206, 'PAID', 'PAYOS', 220000, 4400, 215600, NOW() - INTERVAL '5.1 hours', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '4.2 hours');

-- Đơn 207: Đang rửa xe trong khoang (IN_PROGRESS)
-- Khoang số 1, Khách 6
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, total_price, qr_content, qr_used, arrived_at, wash_started_at, completed_at, staff_id, created_at, updated_at)
VALUES (207, 'BK207', 6, 3, NOW() - INTERVAL '15 minutes', NOW() + INTERVAL '35 minutes', 'IN_PROGRESS', 1, false, NULL, 220000, 'AUTOWASH|BOOKING|BK207|uuid-207', true, NOW() - INTERVAL '20 minutes', NOW() - INTERVAL '15 minutes', NULL, 2, NOW() - INTERVAL '1 hour', NOW());

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (207, 207, 5, 220000, 50);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (207, 207, 'PENDING', 'CASH', 220000, 11000, 209000, NULL, NOW() - INTERVAL '1 hour', NOW());

-- Đơn 208: Khách đã check-in đang xếp hàng đợi (ARRIVED)
-- Đơn Khách 7
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, total_price, qr_content, qr_used, arrived_at, wash_started_at, completed_at, staff_id, created_at, updated_at)
VALUES (208, 'BK208', 7, 4, NOW() - INTERVAL '10 minutes', NOW() + INTERVAL '45 minutes', 'ARRIVED', NULL, false, 'Rửa kỹ lazang', 260000, 'AUTOWASH|BOOKING|BK208|uuid-208', true, NOW() - INTERVAL '5 minutes', NULL, NULL, 3, NOW() - INTERVAL '2 hours', NOW());

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (208, 208, 6, 260000, 55);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (208, 208, 'PAID', 'PAYOS', 260000, 26000, 234000, NOW() - INTERVAL '1.9 hours', NOW() - INTERVAL '2 hours', NOW());

-- Đơn 209: Ca đặt tí nữa mới tới (CONFIRM)
-- Khách 4
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, bay_number, is_late, customer_note, total_price, qr_content, qr_used, arrived_at, wash_started_at, completed_at, staff_id, created_at, updated_at)
VALUES (209, 'BK209', 4, 1, NOW() + INTERVAL '2 hours', NOW() + INTERVAL '2.5 hours', 'CONFIRM', NULL, false, NULL, 100000, 'AUTOWASH|BOOKING|BK209|uuid-209', false, NULL, NULL, NULL, NULL, NOW() - INTERVAL '3 hours', NOW());

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (209, 209, 1, 100000, 30);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (209, 209, 'PAID', 'WALLET', 100000, 0, 100000, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours', NOW());


-- ------------------------------------------------------------------------
-- BƯỚC 5: CHÈN DỮ LIỆU ĐƠN ĐẶT LỊCH TƯƠNG LAI & KIỂM THỬ HỦY (FUTURE BOOKINGS)
-- ------------------------------------------------------------------------

-- Đơn 210: Yêu cầu hủy đang CHỜ DUYỆT (PENDING cancel request)
-- Khách 4, Rửa Premium Wash (180k)
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, total_price, qr_content, qr_used, is_late, created_at, updated_at, cancel_request_status, cancel_request_reason, cancel_requested_by, cancel_requested_at)
VALUES (210, 'BK210', 4, 1, NOW() + INTERVAL '1 day 2 hours', NOW() + INTERVAL '1 day 2.75 hours', 'CONFIRM', 180000, 'AUTOWASH|BOOKING|BK210|uuid-210', false, false, NOW(), NOW(), 'PENDING', 'Khách báo bận đi công tác đột xuất, nhân viên tiếp nhận đề xuất hủy.', 2, NOW());

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (210, 210, 4, 180000, 45);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (210, 210, 'PAID', 'PAYOS', 180000, 0, 180000, NOW() - INTERVAL '1 hour', NOW(), NOW());

-- Đơn 211: Yêu cầu hủy ĐÃ DUYỆT (APPROVED cancel request)
-- Khách 5, Trạng thái CANCELLED
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, total_price, qr_content, qr_used, is_late, created_at, updated_at, cancel_request_status, cancel_request_reason, cancel_requested_by, cancel_requested_at, cancel_request_admin_note)
VALUES (211, 'BK211', 5, 2, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day 50 minutes', 'CANCELLED', 220000, 'AUTOWASH|BOOKING|BK211|uuid-211', true, false, NOW() - INTERVAL '2 days', NOW(), 'APPROVED', 'Thiết bị vòi phun áp lực khoang 1 bị vỡ ống cấp nước.', 3, NOW() - INTERVAL '1 day', 'Admin đã duyệt và tiền được trả lại ví 100% tự động.');

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (211, 211, 5, 220000, 50);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (211, 211, 'REFUNDED', 'PAYOS', 220000, 0, 220000, NOW() - INTERVAL '1 day', NOW() - INTERVAL '2 days', NOW());

-- Đơn 212: Yêu cầu hủy BỊ TỪ CHỐI (REJECTED cancel request)
-- Khách 6, Trạng thái CONFIRM quay lại vận hành
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, total_price, qr_content, qr_used, is_late, created_at, updated_at, cancel_request_status, cancel_request_reason, cancel_requested_by, cancel_requested_at, cancel_request_admin_note)
VALUES (212, 'BK212', 6, 3, NOW() + INTERVAL '2 days', NOW() + INTERVAL '2 days 70 minutes', 'CONFIRM', 450000, 'AUTOWASH|BOOKING|BK212|uuid-212', false, false, NOW(), NOW(), 'REJECTED', 'Thiếu thợ rửa máy chuyên sâu.', 2, NOW(), 'Cửa hàng đã tăng cường nhân viên ca khác sang hỗ trợ. Lịch tiếp tục hoạt động.');

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (212, 212, 8, 450000, 70);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (212, 212, 'PAID', 'PAYOS', 450000, 0, 450000, NOW() - INTERVAL '1 hour', NOW(), NOW());

-- Đơn 213: Đơn hàng tương lai đã thanh toán (CONFIRM)
-- Khách 7
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, total_price, qr_content, qr_used, is_late, created_at, updated_at)
VALUES (213, 'BK213', 7, 4, NOW() + INTERVAL '3 days 4 hours', NOW() + INTERVAL '3 days 5.8 hours', 'CONFIRM', 800000, 'AUTOWASH|BOOKING|BK213|uuid-213', false, false, NOW(), NOW());

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (213, 213, 12, 800000, 110);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (213, 213, 'PAID', 'PAYOS', 800000, 80000, 720000, NOW(), NOW(), NOW());

-- Đơn 214: Đơn hàng tương lai đang chờ thanh toán (PENDING)
-- Khách 5
INSERT INTO bookings (id, booking_code, user_id, vehicle_id, scheduled_start_time, expected_end_time, status, total_price, qr_content, qr_used, is_late, created_at, updated_at)
VALUES (214, 'BK214', 5, 2, NOW() + INTERVAL '2 days 6 hours', NOW() + INTERVAL '2 days 6.6 hours', 'PENDING', 130000, 'AUTOWASH|BOOKING|BK214|uuid-214', false, false, NOW(), NOW());

INSERT INTO booking_details (id, booking_id, service_price_id, actual_price, actual_duration_minutes)
VALUES (214, 214, 2, 130000, 35);

INSERT INTO payments (id, booking_id, payment_status, payment_method, sub_total, discount_amount, final_price, paid_at, created_at, updated_at)
VALUES (214, 214, 'PENDING', 'PAYOS', 130000, 2600, 127400, NULL, NOW(), NOW());

COMMIT;
