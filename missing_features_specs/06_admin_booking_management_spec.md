# Đặc tả Yêu cầu - Quản Lý Đơn Đặt Lịch Của Admin (Admin Booking Management)

Hỗ trợ Admin kiểm soát và quản lý danh sách toàn bộ các lịch đặt xe trong hệ thống, bao gồm phân trang, lọc theo trạng thái, tìm kiếm nâng cao, xem chi tiết hóa đơn/lộ trình và hủy/xóa lịch hẹn.

---

## 1. API Endpoints cần bổ sung/chỉnh sửa

### 1.1. Lấy danh sách Booking (Nâng cấp)
- **Method:** `GET`
- **Path:** `/api/admin/bookings`
- **Query Parameters:**
  - `page`: Số trang (mặc định = 1).
  - `limit`: Số bản ghi mỗi trang (mặc định = 10).
  - `status`: Lọc theo trạng thái đặt lịch (Ví dụ: `PENDING`, `CONFIRM`, `ARRIVED`, `COMPLETED`, `CANCELLED`).
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

### 1.3. Cập nhật trạng thái Booking (Override)
- **Method:** `PUT`
- **Path:** `/api/admin/bookings/{id}/status`
- **Request Body:**
```json
{
  "status": "ARRIVED"
}
```
- **Logic xử lý:** Admin có quyền ghi đè trạng thái của bất kỳ lịch hẹn nào (chuyển sang `ARRIVED`, `COMPLETED`, `CANCELLED`,...). Nếu chuyển sang `CANCELLED`, hệ thống tự động hoàn tiền theo quy định (đối với thanh toán online/ví).

### 1.4. Xóa đơn đặt lịch
- **Method:** `DELETE`
- **Path:** `/api/admin/bookings/{id}`
- **Logic xử lý:** Chuyển trạng thái booking thành `CANCELLED` (hủy đơn) hoặc xóa mềm đơn hàng khỏi hệ thống nếu không phải trạng thái `COMPLETED`.

---

## 2. Thiết kế logic mã nguồn Backend

### 2.1. Tạo DTO trả về danh sách phân trang (`AdminBookingListResponse.java`)
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

### 2.2. Nâng cấp các API trong `AdminController.java`
Thay thế endpoint cũ trong [AdminController.java](file:///c:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/analytics/controller/AdminController.java):
```java
    // 1. Lấy danh sách booking hỗ trợ phân trang và tìm kiếm
    @GetMapping("/bookings")
    public AdminBookingListResponse getBookings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return bookingService.getAdminBookingsWithFilters(page, limit, status, search, startDate, endDate);
    }

    // 2. Lấy chi tiết booking
    @GetMapping("/bookings/{id}")
    public BookingResponse getBookingDetail(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    // 3. Admin cập nhật trạng thái đơn
    @PutMapping("/bookings/{id}/status")
    public BookingResponse updateBookingStatusByAdmin(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        BookingStatus status = BookingStatus.valueOf(body.get("status").toUpperCase());
        return bookingService.updateBookingStatusByAdmin(id, status);
    }

    // 4. Hủy đơn
    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<?> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBookingByAdmin(id);
        return ResponseEntity.ok(Map.of("message", "Đã hủy đơn hàng thành công"));
    }
```
*Lưu ý:* Cần cài đặt câu truy vấn động sử dụng Specification hoặc JPQL động trong `BookingRepository` để thực hiện lọc theo `search` (biển số, họ tên, SĐT) và khoảng ngày `startDate`/`endDate`.
