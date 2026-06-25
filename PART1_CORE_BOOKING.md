# HƯỚNG DẪN PHẦN 1: ĐỒNG BỘ LUỒNG ĐẶT LỊCH CƠ BẢN (CORE BOOKING)

Mục tiêu của phần này là nhận đầy đủ các thông tin thanh toán từ Frontend gửi lên, tính toán chiết khấu (theo hạng thành viên và voucher), tạo và lưu hóa đơn (`Payment`) vào cơ sở dữ liệu.

---

## 1. Cập nhật DTO `CreateBookingRequest.java`

Mở file: [CreateBookingRequest.java](file:///C:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/booking/dto/request/CreateBookingRequest.java)
Bổ sung thêm các trường nhận diện hình thức thanh toán và voucher từ Frontend gửi lên:

```java
package com.autowash.features.booking.dto.request;

import com.autowash.features.booking.enums.PaymentMethod;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreateBookingRequest {

    @NotNull(message = "Vehicle ID không được để trống")
    private Long vehicleId;

    @NotNull(message = "Thời gian đặt lịch không được để trống")
    @Future(message = "Thời gian đặt lịch phải ở tương lai")
    private LocalDateTime scheduledStartTime;

    @NotEmpty(message = "Phải chọn ít nhất 1 dịch vụ")
    private List<Long> serviceIds;

    private String customerNote;

    // Bổ sung các trường mới
    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    private String voucherCode;
}
```

---

## 2. Viết Logic Tính Toán Giá và Tạo Hóa Đơn (`Payment`)

Mở file: [BookingService.java](file:///C:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/booking/service/BookingService.java)

Trong phương thức `createBooking(...)`, sau khi lưu thành công `Booking` và `BookingDetail`, chúng ta tiến hành tính toán tiền và lưu hóa đơn `Payment`.

### Các bước xử lý trong `createBooking`:
1. **Tính tổng tiền dịch vụ (Sub-total)**: Cộng giá tiền của tất cả dịch vụ được chọn.
2. **Tính giảm giá theo hạng thành viên (Tier Discount)**:
   - Lấy thông tin hạng của User (`MEMBER`, `SILVER`, `GOLD`, `PLATINUM`).
   - Lấy tỷ lệ giảm giá từ `TierConfig.getAutoDiscountPercent()`.
   - `tierDiscount = (subTotal * autoDiscountPercent) / 100`.
3. **Áp dụng Voucher giảm giá (nếu có)**:
   - Nếu `request.getVoucherCode()` không rỗng:
     - Tìm voucher trong cơ sở dữ liệu (`CustomerVoucher` thuộc về user này).
     - Kiểm tra tính hợp lệ: Trạng thái phải là `AVAILABLE` và chưa hết hạn (`expiredAt` sau thời gian hiện tại).
     - Lấy thông tin `Promotion` liên kết để tính tiền giảm:
       - Nếu giảm theo số tiền cố định (`discountAmount`): `voucherDiscount = discountAmount`.
       - Nếu giảm theo phần trăm (`discountPercent`): `voucherDiscount = (subTotal * discountPercent) / 100`, khống chế tối đa theo `maxDiscountAmount` (nếu có).
     - Đánh dấu voucher là `USED` và lưu lại.
4. **Tính tiền thực trả (Final Price)**:
   - `finalPrice = subTotal - tierDiscount - voucherDiscount`.
   - Nếu `finalPrice < 0` $\rightarrow$ gán `finalPrice = 0`.
5. **Tạo thực thể `Payment`**:
   - `paymentStatus = PaymentStatus.PENDING` (hoặc `PAID` nếu thanh toán tiền mặt/sau).
   - Lưu thực thể `Payment` vào cơ sở dữ liệu bằng `paymentRepository.save(...)`.

### Đoạn code mẫu thêm vào `createBooking`:
```java
// Tính subTotal và discount
int subTotal = selectedPrices.stream().mapToInt(ServicePrice::getPrice).sum();
int tierDiscount = 0;
int voucherDiscount = 0;

// Tính giảm giá theo Tier
CustomerProfile profile = customer.getCustomerProfile();
if (profile != null && profile.getTierConfig() != null) {
    double discountPercent = profile.getTierConfig().getAutoDiscountPercent().doubleValue();
    tierDiscount = (int) Math.round((subTotal * discountPercent) / 100);
}

CustomerVoucher appliedVoucher = null;
if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
    String vCode = request.getVoucherCode().trim();
    // Tìm voucher khả dụng của khách
    appliedVoucher = customerVoucherRepository.findByUserIdAndVoucherCodeAndStatus(
            customerId, vCode, VoucherStatus.AVAILABLE
    ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Voucher không hợp lệ hoặc đã được sử dụng"
    ));

    // Kiểm tra hết hạn
    if (appliedVoucher.getExpiredAt() != null && appliedVoucher.getExpiredAt().isBefore(LocalDateTime.now())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher đã hết hạn");
    }

    Promotion promo = appliedVoucher.getPromotion();
    if (promo != null) {
        if (promo.getDiscountAmount() != null) {
            voucherDiscount = promo.getDiscountAmount();
        } else if (promo.getDiscountPercent() != null) {
            double pct = promo.getDiscountPercent().doubleValue();
            voucherDiscount = (int) Math.round((subTotal * pct) / 100);
            if (promo.getMaxDiscountAmount() != null && voucherDiscount > promo.getMaxDiscountAmount()) {
                voucherDiscount = promo.getMaxDiscountAmount();
            }
        }
    }
    
    // Đánh dấu voucher đã dùng
    appliedVoucher.setStatus(VoucherStatus.USED);
    appliedVoucher.setUsedAt(LocalDateTime.now());
    customerVoucherRepository.save(appliedVoucher);
}

int finalPrice = Math.max(subTotal - tierDiscount - voucherDiscount, 0);

// Tạo bản ghi Payment
Payment payment = Payment.builder()
        .booking(savedBooking)
        .appliedVoucher(appliedVoucher)
        .paymentMethod(request.getPaymentMethod())
        .subTotal(subTotal)
        .discountAmount(tierDiscount + voucherDiscount)
        .finalPrice(finalPrice)
        .paymentStatus(PaymentStatus.PENDING) // Mặc định chờ thanh toán
        .build();

paymentRepository.save(payment);
```

---

## 3. Xác thực kết quả
* Sau khi thực hiện xong, chạy lại server và gọi API `POST /api/customer/bookings` từ Frontend.
* Kiểm tra trong database SQL Server:
  * Bảng `BOOKINGS` phải có bản ghi mới.
  * Bảng `PAYMENTS` phải tự động sinh ra bản ghi liên kết với `booking_id` tương ứng, lưu đúng số tiền `sub_total` và `final_price`.
  * Nếu áp dụng voucher, trạng thái voucher trong bảng `CUSTOMER_VOUCHERS` phải chuyển sang `USED`.
