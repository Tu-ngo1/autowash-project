# HƯỚNG DẪN PHẦN 2: TÍCH HỢP VÍ ĐIỆN TỬ VÀ CỔNG THANH TOÁN (WALLET & PAYOS)

Mục tiêu của phần này là lập trình luồng thanh toán thực tế khi đặt lịch bằng Ví điện tử (`WALLET`) (trừ tiền ví, lưu giao dịch) và luồng thanh toán online qua PayOS (`PAYOS`) (tạo link thanh toán, xử lý callback/webhook).

---

## 1. Thanh toán bằng Ví điện tử (`WALLET`)

Khi khách hàng nhấn nút đặt lịch và chọn hình thức thanh toán bằng Ví, hệ thống cần trừ số dư ví và ghi nhận lịch sử giao dịch.

### Logic lập trình trong `BookingService.createBooking(...)`:
Nếu `request.getPaymentMethod() == PaymentMethod.WALLET`:
1. **Tìm ví của user**: Truy vấn bảng `WALLETS` theo `userId` của khách hàng.
2. **Kiểm tra số dư**: So sánh `wallet.getBalance()` với `finalPrice`. Nếu không đủ tiền $\rightarrow$ Ném ra ngoại lệ `ResponseStatusException` (HttpStatus.BAD_REQUEST, "Số dư tài khoản ví không đủ. Vui lòng nạp thêm tiền.").
3. **Trừ tiền ví**: `wallet.setBalance(wallet.getBalance().subtract(BigDecimal.valueOf(finalPrice)))`.
4. **Lưu lịch sử giao dịch**: Tạo bản ghi `WalletTransaction` với:
   - `transactionType = WalletTransactionType.PAYMENT`
   - `amount = -finalPrice` (số tiền âm thể hiện giao dịch chi ra)
   - `description = "Thanh toán lịch hẹn rửa xe: " + booking.getBookingCode()`
5. **Cập nhật hóa đơn**:
   - `payment.setPaymentStatus(PaymentStatus.PAID)`
   - `payment.setPaidAt(LocalDateTime.now())`
6. **Cập nhật lịch hẹn**: Trạng thái booking chuyển thành `PENDING` (hoặc `CONFIRMED` tùy thiết kế).

### Đoạn code mẫu:
```java
if (request.getPaymentMethod() == PaymentMethod.WALLET) {
    Wallet wallet = walletRepository.findByUserId(customerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ví của khách hàng"));

    BigDecimal priceDecimal = BigDecimal.valueOf(finalPrice);
    if (wallet.getBalance().compareTo(priceDecimal) < 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số dư ví không đủ. Vui lòng nạp tiền.");
    }

    // Trừ số dư ví
    wallet.setBalance(wallet.getBalance().subtract(priceDecimal));
    walletRepository.save(wallet);

    // Ghi nhận giao dịch ví
    WalletTransaction transaction = WalletTransaction.builder()
            .wallet(wallet)
            .amount(priceDecimal.negate())
            .transactionType(WalletTransactionType.PAYMENT)
            .description("Thanh toán lịch hẹn " + savedBooking.getBookingCode())
            .createdAt(LocalDateTime.now())
            .build();
    walletTransactionRepository.save(transaction);

    // Cập nhật trạng thái thanh toán thành đã trả tiền
    payment.setPaymentStatus(PaymentStatus.PAID);
    payment.setPaidAt(LocalDateTime.now());
    paymentRepository.save(payment);
}
```

---

## 2. Thanh toán bằng Cổng thanh toán trực tuyến (`PAYOS`)

Khi khách hàng chọn thanh toán qua PayOS:
1. Đơn hàng ban đầu có trạng thái `PENDING` (Chờ thanh toán).
2. Hệ thống gọi cổng PayOS để lấy link thanh toán QR (`checkoutUrl`).
3. Trả `checkoutUrl` về cho Frontend chuyển hướng người dùng sang trang thanh toán ngân hàng.
4. Xử lý webhook PayOS gọi về khi giao dịch thành công.

### A. Sinh link thanh toán PayOS khi đặt lịch
Trong `BookingService.createBooking(...)` (hoặc trong Controller):
* Khởi tạo PayOS SDK (cần cấu hình `client_id`, `api_key`, `checksum_key` trong `application.properties`).
* Tạo đối tượng `PaymentData` gồm các thông tin: `orderCode` (mã số ngẫu nhiên hoặc ID booking), `amount` (chính là `finalPrice`), `description` (Mã booking), `cancelUrl`, `returnUrl`.
* Gọi `payOS.createPaymentLink(paymentData)`.
* Lấy `checkoutUrl` trả về trong response DTO gửi về Frontend.

### B. Tạo API nhận Webhook từ PayOS khi thanh toán thành công
Tạo một REST Controller mới: `/api/payment/payos-webhook` (Cho phép truy cập không cần token - permitAll trong SecurityConfig).

```java
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @PostMapping("/payos-webhook")
    public ResponseEntity<?> receiveWebhook(@RequestBody PayosWebhookRequest webhookData) {
        // 1. Xác thực chữ ký webhook từ PayOS gửi đến để tránh giả mạo
        // (Sử dụng hàm verifyWebhookData của PayOS SDK)
        
        // 2. Nếu xác thực thành công và trạng thái là SUCCESS:
        if ("SUCCESS".equalsIgnoreCase(webhookData.getData().getStatus())) {
            String bookingCode = webhookData.getData().getDescription(); // Lấy mã booking từ description
            
            Booking booking = bookingRepository.findByBookingCode(bookingCode)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));
            
            if (booking.getStatus() == BookingStatus.PENDING && booking.getPayment() != null) {
                Payment payment = booking.getPayment();
                
                // Cập nhật trạng thái hóa đơn thành đã trả
                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
                
                // Cập nhật trạng thái lịch hẹn thành CONFIRMED (hoặc giữ nguyên PENDING)
                booking.setStatus(BookingStatus.PENDING);
                bookingRepository.save(booking);
            }
        }
        
        return ResponseEntity.ok(new MessageResponse("Webhook processed successfully"));
    }
}
```

---

## 3. Xác thực kết quả
* **Test Ví**: Đăng nhập bằng tài khoản khách hàng có tiền trong ví. Tiến hành đặt lịch chọn Ví $\rightarrow$ Hệ thống trừ tiền thành công, chuyển hướng thẳng về lịch sử đặt lịch.
* **Test PayOS**: Chọn thanh toán PayOS $\rightarrow$ Hệ thống trả về link thanh toán $\rightarrow$ Frontend chuyển hướng đến giao diện thanh toán QR của ngân hàng.
