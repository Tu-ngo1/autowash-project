# Đặc tả Yêu cầu - Quản Lý Đơn Đặt Lịch Của Admin (Admin Booking Management)

Hỗ trợ Admin kiểm soát và quản lý danh sách toàn bộ các lịch đặt xe trong hệ thống, bao gồm phân trang, lọc theo trạng thái, tìm kiếm nâng cao, xem chi tiết hóa đơn/lộ trình và hủy/xóa lịch hẹn. Bổ sung tính năng Staff đề xuất hủy lịch và Admin phê duyệt.

---

## 1. API Endpoints cần bổ sung/chỉnh sửa

### 1.1. Lấy danh sách Booking (Nâng cấp)
- **Method:** `GET`
- **Path:** `/api/admin/bookings`
- **Query Parameters:**
  - `page`: Số trang (mặc định = 1).
  - `limit`: Số bản ghi mỗi trang (mặc định = 10).
  - `status`: Lọc theo trạng thái đặt lịch (Ví dụ: `PENDING`, `CONFIRM`, `ARRIVED`, `COMPLETED`, `CANCELLED`).
  - `cancelRequestStatus`: Lọc theo trạng thái yêu cầu hủy (Ví dụ: `PENDING`, `APPROVED`, `REJECTED`).
  - `search`: Tìm kiếm theo biển số xe, họ tên khách hàng, số điện thoại hoặc mã đơn đặt lịch (`bookingCode`).
  - `startDate`, `endDate`: Khoảng thời gian đặt lịch (`YYYY-MM-DD`).
- **Response Body:**
```json
{
  "bookings": [
    {
      "id": 1,
      "bookingCode": "BK-260615-001",
      "customerName": "Nguyễn Văn A",
      "customerPhone": "0901234567",
      "customerEmail": "customer@example.com",
      "vehicleLicensePlate": "51F-123.45",
      "tierLevel": "GOLD",
      "services": ["Rửa xe cơ bản", "Hút bụi"],
      "status": "PENDING",
      "cancelRequestStatus": "PENDING",
      "cancelRequestReason": "Khách hàng gọi báo bận đột xuất",
      "paymentStatus": "UNPAID",
      "paymentMethod": "CASH",
      "scheduledStartTime": "2026-06-15T09:00:00",
      "totalPrice": 150000
    }
  ],
  "total": 24
}
```

### 1.2. Xem chi tiết Booking
- **Method:** `GET`
- **Path:** `/api/admin/bookings/{id}`
- **Response Body:** Trả về đối tượng `BookingResponse` đầy đủ thông tin (như cấu trúc trên).

### 1.3. Cập nhật trạng thái Booking trực tiếp (Override)
- **Method:** `PUT`
- **Path:** `/api/admin/bookings/{id}/status`
- **Request Body:**
```json
{
  "status": "ARRIVED"
}
```
- **Logic xử lý:** Admin có quyền ghi đè trạng thái của bất kỳ lịch hẹn nào (chuyển sang `ARRIVED`, `COMPLETED`, `CANCELLED`,...). 
  - Nếu Admin chuyển trực tiếp sang `CANCELLED`, hệ thống tự động hoàn tiền **100%** vào ví (Wallet) đối với các đơn thanh toán trực tuyến/ví (do đây là hủy trực tiếp từ phía Admin/Cửa hàng).

### 1.4. Xóa/Hủy đơn đặt lịch trực tiếp
- **Method:** `DELETE`
- **Path:** `/api/admin/bookings/{id}`
- **Logic xử lý:** Chuyển trạng thái booking thành `CANCELLED` (hủy đơn) hoặc xóa mềm đơn hàng khỏi hệ thống nếu không phải trạng thái `COMPLETED`. Tự động hoàn tiền **100%** vào ví khách hàng đối với thanh toán online/ví.

### 1.5. Staff gửi yêu cầu hủy lịch hẹn (Yêu cầu phải có lý do)
- **Method:** `POST`
- **Path:** `/api/staff/bookings/{id}/cancel-request`
- **Request Body:**
```json
{
  "reason": "Máy bơm cao áp khoang rửa chuyên sâu đột ngột gặp sự cố, không đủ thiết bị vận hành"
}
```
- **Logic xử lý:**
  - Chỉ cho phép gửi yêu cầu hủy cho đơn ở các trạng thái chưa bắt đầu dịch vụ: `PENDING`, `CONFIRM`, `ARRIVED`. Không cho phép đơn đang rửa (`IN_PROGRESS`), hoặc đã hoàn thành (`WASHED`, `COMPLETED`, `CANCELLED`).
  - Kiểm tra lý do (`reason`) không được để trống hoặc rỗng.
  - Cập nhật các trường thông tin yêu cầu hủy trên entity `Booking`:
    - `cancelRequestStatus` = `PENDING`
    - `cancelRequestReason` = `reason`
    - `cancelRequestedBy` = Nhân viên đang đăng nhập
    - `cancelRequestedAt` = `LocalDateTime.now()`
  - Chặn các thao tác vận hành khác của Staff (Check-in, assign vào khoang rửa) đối với booking có `cancelRequestStatus = PENDING`.

### 1.6. Admin phê duyệt yêu cầu hủy từ Staff
- **Method:** `POST`
- **Path:** `/api/admin/bookings/{id}/cancel-request/approve`
- **Response Body:** Trả về đối tượng `BookingResponse` đã được cập nhật trạng thái hủy.
- **Logic xử lý:**
  - Xác nhận booking có `cancelRequestStatus` là `PENDING`.
  - Cập nhật `cancelRequestStatus` = `APPROVED`.
  - Cập nhật trạng thái đơn hàng `status` = `CANCELLED`.
  - Thực hiện hoàn tiền **100%** số tiền vào ví của khách hàng (`processRefund(booking, 1.0)`) do sự cố phát sinh từ cửa hàng.

### 1.7. Admin bác bỏ yêu cầu hủy từ Staff
- **Method:** `POST`
- **Path:** `/api/admin/bookings/{id}/cancel-request/reject`
- **Request Body:**
```json
{
  "adminNote": "Cửa hàng đã điều phối thiết bị dự phòng, yêu cầu tiếp tục phục vụ khách hàng."
}
```
- **Logic xử lý:**
  - Xác nhận booking có `cancelRequestStatus` là `PENDING`.
  - Cập nhật `cancelRequestStatus` = `REJECTED`.
  - Ghi nhận `cancelRequestAdminNote` = `adminNote`.
  - Giữ nguyên trạng thái đơn hàng (`status`), không thực hiện hoàn tiền. Lịch hẹn quay lại luồng vận hành bình thường.

---

## 2. Thiết kế logic mã nguồn Backend

### 2.1. Thêm Enum `CancelRequestStatus.java`
```java
package com.autowash.features.booking.enums;

public enum CancelRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
```

### 2.2. Bổ sung các thuộc tính vào Entity `Booking.java`
```java
    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_request_status")
    private CancelRequestStatus cancelRequestStatus;

    @Column(name = "cancel_request_reason", columnDefinition = "TEXT")
    private String cancelRequestReason;

    @ManyToOne
    @JoinColumn(name = "cancel_requested_by")
    private User cancelRequestedBy;

    @Column(name = "cancel_requested_at")
    private LocalDateTime cancelRequestedAt;

    @Column(name = "cancel_request_admin_note", columnDefinition = "TEXT")
    private String cancelRequestAdminNote;
```

### 2.3. Tạo DTO trả về danh sách phân trang (`AdminBookingListResponse.java`)
```java
package com.autowash.features.booking.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AdminBookingListResponse {
    private List<BookingResponse> bookings;
    private Long total;
}
```

### 2.4. Cập nhật các API trong `AdminController.java`
```java
    // 1. Lấy danh sách booking hỗ trợ phân trang, tìm kiếm và lọc trạng thái yêu cầu hủy
    @GetMapping("/bookings")
    public AdminBookingListResponse getBookings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) CancelRequestStatus cancelRequestStatus,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return bookingService.getAdminBookingsWithFilters(page, limit, status, cancelRequestStatus, search, startDate, endDate);
    }

    // 2. Lấy chi tiết booking
    @GetMapping("/bookings/{id}")
    public BookingResponse getBookingDetail(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    // 3. Admin cập nhật trạng thái đơn trực tiếp (Override)
    @PutMapping("/bookings/{id}/status")
    public BookingResponse updateBookingStatusByAdmin(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        BookingStatus status = BookingStatus.valueOf(body.get("status").toUpperCase());
        return bookingService.updateBookingStatusByAdmin(id, status);
    }

    // 4. Admin hủy đơn trực tiếp
    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<?> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBookingByAdmin(id);
        return ResponseEntity.ok(Map.of("message", "Đã hủy đơn hàng thành công và hoàn tiền 100% vào ví"));
    }

    // 5. Admin duyệt yêu cầu hủy từ Staff
    @PostMapping("/bookings/{id}/cancel-request/approve")
    public BookingResponse approveCancelRequest(@PathVariable Long id) {
        return bookingService.approveCancelRequest(id);
    }

    // 6. Admin bác bỏ yêu cầu hủy từ Staff
    @PostMapping("/bookings/{id}/cancel-request/reject")
    public BookingResponse rejectCancelRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String adminNote = body.get("adminNote");
        return bookingService.rejectCancelRequest(id, adminNote);
    }
```

### 2.5. Thêm API yêu cầu hủy trong `StaffController.java`
```java
    // Staff gửi yêu cầu hủy kèm lý do
    @PostMapping("/bookings/{id}/cancel-request")
    public BookingResponse requestCancelBooking(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String reason = body.get("reason");
        return bookingService.createCancelRequestByStaff(id, reason);
    }
```

*Lưu ý:* Khi cập nhật truy vấn JPQL/Specification động trong `BookingRepository`, cần bổ sung trường lọc `cancelRequestStatus` và đảm bảo các hàm Check-in / Assign khoang rửa sẽ chặn các booking có `cancelRequestStatus = PENDING`.
