# Tài liệu Yêu cầu Phát triển Backend (BE) - Quầy Đặt Lịch & Tiếp Nhận Khách Vãng Lai

Tài liệu này hướng dẫn cách triển khai các API ở phía Backend để phục vụ tính năng đặt lịch nhanh cho khách vãng lai từ nhân viên (Staff).

---

## 1. Các API cần xây dựng trong `StaffController.java`

Bạn cần bổ sung các endpoint sau vào [StaffController.java](file:///c:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/booking/controller/StaffController.java) (đường dẫn gốc lớp `@RequestMapping("/api/staff")`):

### 1.1. Tra cứu thông tin khách hàng/xe
* **Mô tả:** Hỗ trợ nhân viên tra cứu nhanh thông tin khách hàng cũ qua SĐT hoặc biển số xe để tự điền thông tin và áp dụng ưu đãi hạng thành viên.
* **Method:** `GET`
* **Path:** `/customers/search`
* **Query Params:** `@RequestParam String query` (SĐT hoặc biển số xe)
* **Kết quả xử lý:**
  * Tìm trong bảng `USERS` theo số điện thoại hoặc bảng `VEHICLES` theo biển số xe.
  * Trả về thông tin khách hàng bao gồm: Tên, SĐT, Hạng thành viên (`tierLevel`), Điểm tích lũy, các biển số xe đã đăng ký.

### 1.2. Lấy dữ liệu dịch vụ và slot trống hôm nay
* **Mô tả:** Trả về danh sách dịch vụ và các khung giờ trống của ngày hôm nay để hiển thị lên lưới chọn của nhân viên.
* **Method:** `GET`
* **Path:** `/bookings/walk-in/data`
* **Query Params:** 
  * `@RequestParam VehicleSize carSize` (Kích cỡ xe của khách: `SMALL`, `MEDIUM`, `LARGE`)
* **Kết quả xử lý:**
  * Gọi dịch vụ lấy danh sách dịch vụ theo size xe (`washService.getServicesByVehicleSize(carSize)`).
  * Gọi dịch vụ tính các khung giờ trống cho ngày hôm nay (`bookingService.getAvailableSlots(LocalDate.now(), 90)`).
  * Trả về đối tượng `BookingDataResponse` chứa danh sách dịch vụ và danh sách slot trống.

### 1.3. Tạo đặt lịch vãng lai trực tiếp
* **Mô tả:** Nhận thông tin từ nhân viên, tạo booking, tự động thiết lập trạng thái đã thanh toán trực tiếp và chuyển xe thẳng vào hàng đợi.
* **Method:** `POST`
* **Path:** `/bookings/walk-in`
* **Request Body:** `@Valid @RequestBody WalkInBookingRequest request`

---

## 2. Tạo Request DTO mới: `WalkInBookingRequest.java`
Tạo file tại gói `com.autowash.features.booking.dto.request`:
```java
package com.autowash.features.booking.dto.request;

import com.autowash.features.booking.enums.PaymentMethod;
import com.autowash.features.car.enums.VehicleSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WalkInBookingRequest {
    @NotBlank(message = "Tên khách hàng không được để trống")
    private String customerName;

    private String customerPhone; // Có thể để trống đối với khách vãng lai vãng lai

    @NotBlank(message = "Biển số xe không được để trống")
    private String licensePlate;

    @NotNull(message = "Kích cỡ xe không được để trống")
    private VehicleSize vehicleSize;

    @NotNull(message = "Thời gian đặt lịch không được để trống")
    private LocalDateTime scheduledStartTime;

    @NotEmpty(message = "Phải chọn ít nhất 1 dịch vụ")
    private List<Long> serviceIds;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod; // CASH hoặc BANK_TRANSFER

    private String customerNote;
}
```

---

## 3. Triển khai logic nghiệp vụ trong `BookingService.java`

Viết phương thức `createWalkInBooking(WalkInBookingRequest request)` tại [BookingService.java](file:///c:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/booking/service/BookingService.java) với các bước xử lý sau:

1. **Xử lý tài khoản khách hàng:**
   * Nếu `customerPhone` được cung cấp, tìm kiếm `User` trong DB. Nếu tìm thấy, sử dụng User đó để được hưởng chiết khấu hạng thành viên.
   * Nếu không cung cấp hoặc không tìm thấy, hệ thống sẽ sử dụng một tài khoản "Khách vãng lai mặc định" (Ví dụ tài khoản hệ thống tự tạo trước có email `walkin@autowash.com`) hoặc tạo nhanh một User mới dạng vãng lai.
2. **Xử lý xe (Vehicle):**
   * Kiểm tra xem xe có biển số `licensePlate` đã tồn tại trong DB chưa.
   * Nếu chưa tồn tại, tạo mới bản ghi `Vehicle` với biển số đó, gán `vehicleSize` và liên kết với User vừa xác định ở trên.
3. **Tính toán chi phí:**
   * Tính tổng tiền gốc của các dịch vụ trong `serviceIds`.
   * Áp dụng chiết khấu hạng thành viên (nếu có).
   * Tính toán giá cuối cùng (`finalPrice`).
4. **Tạo Booking & Payment:**
   * Khởi tạo đối tượng `Booking` với các trường dữ liệu.
   * Đặt trạng thái booking trực tiếp thành **`ARRIVED`** (Xe đã có mặt tại cửa hàng vì được tiếp nhận trực tiếp).
   * Đánh dấu `qrUsed = true`.
   * Khởi tạo đối tượng `Payment`. Vì nhân viên đã xác nhận giao dịch trực tiếp bằng tiền mặt hoặc chuyển khoản tại quầy, đặt trạng thái thanh toán là **`PAID`** và gán `paidAt = LocalDateTime.now()`.
5. **Lưu dữ liệu và phản hồi:**
   * Lưu `Booking` và `Payment` vào cơ sở dữ liệu.
   * Chuyển đổi thành `BookingResponse` và trả về.
