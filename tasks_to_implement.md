# Danh Sách Các Chức Năng Cần Phát Triển (Tự Thực Hiện)

Tài liệu này tóm tắt các tính năng nâng cao mà bạn (USER) sẽ tự tay lập trình ở phần Backend để hoàn thiện luồng Đặt lịch, Quản lý khoang rửa xe và tích hợp Ví điện tử.

---

## 1. Thuật Toán Tính Khung Giờ Trống Động (`getAvailableSlots`)

Nhiệm vụ là viết logic cho phương thức `getAvailableSlots(LocalDate date)` trong [BookingService.java](file:///src/main/java/com/autowash/features/booking/service/BookingService.java).

### Các bước thuật toán gợi ý:
1. **Lấy cấu hình ngày:** 
   - Gọi `dailyOperationsConfigRepository.findByConfigDate(date)`.
   - Nếu có cấu hình tùy chỉnh: Lấy `openTime`, `closeTime`, `bayCount`, `isActive`.
   - Nếu không có: Sử dụng cấu hình mặc định (Mở: `08:00`, Đóng: `18:00`, Khoang rửa: `2`, Trạng thái: `Hoạt động`).
2. **Kiểm tra ngày nghỉ:** Nếu ngày đó có trạng thái `isActive = false` $\rightarrow$ Trả về danh sách trống ngay lập tức.
3. **Sinh các ca đặt dự kiến (Interval 1h30m):**
   - Bắt đầu từ `openTime`, cộng thêm 90 phút cho mỗi ca tiếp theo cho đến khi ca đó vượt quá `closeTime`.
   - Ví dụ: `08:00 - 09:30`, `09:30 - 11:00`, `11:00 - 12:30`,...
4. **Kiểm tra tính khả dụng của mỗi ca:**
   - So sánh thời gian bắt đầu ca với thời gian hiện tại cộng 30 phút (Buffer time). Ca trong quá khứ hoặc quá sát giờ hiện tại sẽ không khả dụng.
   - **Đếm số khoang bận:** Duyệt qua danh sách booking hoạt động trong ngày đó, tìm xem tại thời điểm bắt đầu ca (`slotStart`), có bao nhiêu xe đang được rửa đồng thời (dựa trên thời gian bắt đầu và tổng thời lượng dịch vụ của từng đơn).
   - Nếu số khoang đang bận **nhỏ hơn** `bayCount` $\rightarrow$ Ca này khả dụng (`available = true`). Ngược lại $\rightarrow$ Khóa ca (`available = false`).
5. **Chống tranh chấp (Race Condition):**
   - Thêm annotation `@Lock(LockModeType.PESSIMISTIC_WRITE)` vào phương thức `findByConfigDate` trong `DailyOperationsConfigRepository` để khóa dòng cấu hình khi kiểm tra, tránh 2 khách hàng đặt trùng slot cuối cùng.

---

## 2. Hệ Thống Ví Điện Tử Tích Hợp Hoàn Tiền Tự Động (Online Wallet & Instant Refund)

Thay vì hoàn tiền qua chuyển khoản thủ công phức tạp, toàn bộ dòng tiền thanh toán và hoàn tiền sẽ được tự động xử lý thông qua ví điện tử tích hợp trong hệ thống.

### A. Thiết kế Cơ sở dữ liệu (Entity & Repository):
1. **Bảng `WALLETS`:** Lưu số dư hiện tại của từng người dùng (`balance`). Mối quan hệ 1-1 với bảng `USERS`.
2. **Bảng `WALLET_TRANSACTIONS`:** Lưu lịch sử giao dịch vào/ra của ví (Nạp tiền, Thanh toán lịch, Hoàn tiền hủy lịch).

### B. Các luồng nghiệp vụ cần lập trình:

#### 1. Nạp tiền vào ví (Deposit):
* Tạo link thanh toán PayOS để khách nạp tiền vào ví. 
* Khi webhook PayOS báo thành công $\rightarrow$ Cộng tiền vào bảng `WALLETS` của user và ghi nhận lịch sử nạp ở bảng `WALLET_TRANSACTIONS` (Loại giao dịch: `DEPOSIT`).

#### 2. Thanh toán bằng ví (Payment):
* Khi tạo booking mới: Kiểm tra số dư ví có $\ge$ giá trị đơn hàng không.
* Nếu đủ: Trừ tiền trực tiếp trong ví, ghi nhận giao dịch `PAYMENT`, chuyển trạng thái booking sang `CONFIRM` và payment sang `PAID`.

#### 3. Hoàn tiền tự động lập tức (Instant Refund):
* Khi khách hàng bấm hủy đặt lịch:
  1. Kiểm tra xem thời gian hủy có trước giờ hẹn ít nhất 60 phút (`CANCEL_BUFFER_MINUTES = 60`) hay không.
  2. Nếu **hợp lệ**:
     - Chuyển trạng thái Booking thành `CANCELLED`, Payment thành `REFUNDED`.
     - **Tự động cộng lại 100% số tiền** vào số dư ví của khách hàng (`WALLETS.balance = balance + refundAmount`).
     - Ghi nhận lịch sử giao dịch hoàn tiền (`WALLET_TRANSACTIONS` - Loại giao dịch: `REFUND`).
  3. Nếu **không hợp lệ (hủy trễ)**:
     - Chuyển trạng thái Booking thành `CANCELLED`, Payment thành `FAILED` (mất tiền cọc, không cộng lại tiền vào ví).
