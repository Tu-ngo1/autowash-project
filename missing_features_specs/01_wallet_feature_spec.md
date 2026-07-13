# Đặc tả Yêu cầu - Phân Hệ Ví Điện Tử (Wallet Feature)

Phân hệ này quản lý số dư tài khoản của khách hàng, cho phép nạp tiền qua PayOS, thanh toán lịch đặt bằng số dư ví, và nhận tiền hoàn tự động khi hủy lịch.

---

## 1. Yêu cầu Cơ sở dữ liệu & Cấu hình
- Đảm bảo thực thể `Wallet` tự động được khởi tạo với số dư `0` ngay khi tài khoản khách hàng (`CUSTOMER`) được đăng ký thành công.
- Cấu hình PayOS SDK trong `application.properties` đã sẵn sàng, cần tái sử dụng cấu hình này.

---

## 2. API Endpoints cần bổ sung/chỉnh sửa

### 2.1. Nạp tiền vào ví (Wallet Deposit)
- **Method:** `POST`
- **Path:** `/api/customer/wallet/deposit`
- **Request Body:**
```json
{
  "amount": 200000
}
```
- **Logic xử lý:**
  1. Kiểm tra xác thực khách hàng, lấy `userId`.
  2. Tìm kiếm hoặc tự động khởi tạo ví `Wallet` của khách hàng nếu chưa có.
  3. Giá trị nạp tối thiểu là `10,000đ`.
  4. Tạo mã `orderCode` của PayOS đại diện cho giao dịch nạp tiền. Để phân biệt với mã Booking ID (cũng được gửi lên PayOS dạng Long), ta có thể sử dụng cấu trúc mã hóa số dư:
     `long orderCode = 100000000000000000L + (userId * 1000000000L) + (System.currentTimeMillis() % 1000000000L);`
  5. Gọi PayOS SDK để tạo link thanh toán với thông tin: `orderCode`, `amount`, `description` (Ví dụ: `"Nap vi [userId]"`), `returnUrl`, `cancelUrl`.
  6. Trả về `checkoutUrl` để Frontend chuyển hướng người dùng sang trang thanh toán.
- **Response Body:**
```json
{
  "checkoutUrl": "https://pay.payos.vn/web/..."
}
```

### 2.2. Lấy danh sách giao dịch ví (Get Transactions)
- **Method:** `GET`
- **Path:** `/api/customer/wallet/transactions`
- **Logic xử lý:**
  1. Lấy thông tin khách hàng đang đăng nhập.
  2. Lấy `Wallet` của khách hàng, sau đó truy vấn danh sách `WalletTransaction` sắp xếp giảm dần theo thời gian tạo.
- **Response Body:**
```json
[
  {
    "id": 1,
    "amount": 200000,
    "transactionType": "DEPOSIT",
    "description": "Nạp tiền vào ví qua PayOS",
    "createdAt": "2026-06-24T15:30:00"
  },
  {
    "id": 2,
    "amount": -150000,
    "transactionType": "PAYMENT",
    "description": "Thanh toán lịch đặt BK-A8D9F",
    "createdAt": "2026-06-24T16:00:00"
  }
]
```

### 2.3. Bổ sung số dư ví vào thông tin Profile & User
- Cập nhật DTO `ProfileResponse` và `UserResponse` thêm trường:
```java
private Integer walletBalance;
```
- Cập nhật mapper `ProfileMapper` và `UserMapper` để lấy số dư thực tế từ `Wallet` gán vào DTO. Nếu user chưa có thực thể `Wallet`, mặc định trả về `0`.

---

## 3. Cập nhật Webhook Xử lý Nạp tiền thành công
Trong [PaymentWebhookController.java](file:///c:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/booking/controller/PaymentWebhookController.java):
- Cập nhật logic hàm `receiveWebhook`:
```java
long orderCode = data.getOrderCode();
if (orderCode >= 100000000000000000L) {
    // Đây là giao dịch nạp tiền vào ví!
    long userId = (orderCode - 100000000000000000L) / 1000000000L;
    
    // Tìm ví của user
    Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ví"));
            
    // Kiểm tra trùng lặp giao dịch (Idempotency) để tránh cộng tiền 2 lần
    String txDescription = "Nạp tiền qua PayOS - GD " + orderCode;
    boolean txExists = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId())
            .stream()
            .anyMatch(tx -> txDescription.equals(tx.getDescription()));
            
    if (!txExists && "00".equals(data.getCode())) {
        // Cộng tiền vào ví
        wallet.setBalance(wallet.getBalance() + data.getAmount());
        walletRepository.save(wallet);
        
        // Ghi giao dịch ví
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(data.getAmount())
                .transactionType(WalletTransactionType.DEPOSIT)
                .description(txDescription)
                .build();
        walletTransactionRepository.save(transaction);
    }
} else {
    // Đây là giao dịch đặt lịch (mã gốc bookingId)
    // Giữ nguyên logic xử lý booking cũ...
}
```
