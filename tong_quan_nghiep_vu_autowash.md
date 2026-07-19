# Báo Cáo Tổng Quan Nghiệp Vụ Hệ Thống Autowash

Báo cáo này tổng hợp kết quả rà soát, cấu trúc lập trình và kịch bản vận hành của **tất cả 8 phân hệ nghiệp vụ nâng cao** trong hệ thống Autowash (Backend & Frontend) để phục vụ quá trình nghiệm thu và chạy thử.

---

## 📁 1. BẢN ĐỒ PHÂN BỔ MÃ NGUỒN NGHIỆP VỤ

Dưới đây là sơ đồ tóm tắt các tệp tin cốt lõi chịu trách nhiệm xử lý nghiệp vụ cho từng phân hệ:

| Phân hệ nghiệp vụ | File Backend cốt lõi | File Frontend cốt lõi |
| :--- | :--- | :--- |
| **01. Ví điện tử (Wallet)** | `Wallet.java`, `WalletService.java`, `WalletController.java` | `Wallet.jsx`, `MyWallet.jsx` |
| **02. Đánh giá (Reviews)** | `Review.java`, `ReviewService.java`, `ReviewController.java` | `Dashboard.jsx`, `ReviewModal.jsx` |
| **03. Đổi Voucher (Loyalty)** | `Promotion.java`, `CustomerVoucher.java`, `VoucherService.java` | `LoyaltyDashboard.jsx`, `VoucherShop.jsx` |
| **04. Ngày nghỉ (Operations)** | `DailyOperationsConfig.java`, `OperationsConfigService.java` | `AdminOperations.jsx` |
| **05. CRUD Dịch vụ (Services)** | `Service.java`, `ServicePrice.java`, `ServiceController.java` | `AdminServices.jsx` |
| **06. Quản lý Đặt lịch (Bookings)** | `Booking.java`, `BookingService.java`, `AdminController.java` | `AdminBookings.jsx`, `AdminBookingsTable.jsx` |
| **07. Quản lý User (Users)** | `User.java`, `UserService.java`, `UserController.java` | `AdminUsers.jsx` |
| **08. Hạng thành viên (Tiers)** | `TierConfig.java`, `TierConfigService.java` | `AdminPromotionTier.jsx` |

---

## ⚙️ 2. CHI TIẾT NGHIỆP VỤ & CƠ CHẾ RÀNG BUỘC TỰ ĐỘNG

### 💳 Phân hệ 01: Ví điện tử & Tích hợp PayOS
* **Luồng nạp tiền (Deposit):**
  * Khách hàng yêu cầu nạp tiền trực tuyến $\rightarrow$ BE tạo link thanh toán PayOS $\rightarrow$ Webhook nhận tín hiệu thanh toán thành công $\rightarrow$ Tự động cộng tiền vào ví và ghi nhận giao dịch ví loại `DEPOSIT`.
* **Luồng trừ tiền và hoàn tiền tự động (Payment & Refund):**
  * Khách hàng đặt lịch hẹn có thể chọn trừ tiền trực tiếp từ ví (sinh giao dịch `PAYMENT`).
  * Khách hàng chủ động hủy lịch trước giờ hẹn tối thiểu 60 phút $\rightarrow$ Hệ thống tự động chuyển trạng thái đơn sang `CANCELLED`, hoàn cọc 100% số tiền về ví ngay lập tức (sinh giao dịch `REFUND`).
  * Khách hàng hủy lịch trễ (dưới 60 phút) $\rightarrow$ Tiền cọc bị phạt 100%, trạng thái thanh toán chuyển sang `FAILED` và không được hoàn tiền ví.

### ⭐ Phân hệ 02: Đánh giá chất lượng dịch vụ (Reviews)
* **Quy tắc hiển thị:** Khách hàng chỉ có thể đánh giá các đơn đặt lịch có trạng thái `COMPLETED` và chưa được đánh giá trước đó.
* **Quy trình hoạt động:** Dashboard của khách hàng sẽ quét tìm các đơn hoàn tất rửa xe để tự động hiển thị Modal đánh giá $\rightarrow$ Lưu số sao (1-5) và bình luận phản hồi của khách hàng gửi lên cơ sở dữ liệu.

### 🏆 Phân hệ 03 & 08: Điểm thưởng & Voucher thành viên (Loyalty & Tiers)
* **Ma trận điểm thăng hạng:**
  * Thành viên hạng `MEMBER` ($< 1000$ điểm) $\rightarrow$ Hạng `SILVER` ($1000 - 3000$ điểm) $\rightarrow$ Hạng `GOLD` ($3000 - 6000$ điểm) $\rightarrow$ Hạng `PLATINUM` ($> 6000$ điểm).
* **Quy tắc đổi Voucher:** 
  * Điểm tích lũy (Reward Points) của khách hàng có thể dùng để đổi lấy code giảm giá trong kho Voucher. Hệ thống kiểm tra xem điểm hiện tại có đủ và hạng thành viên có khớp với điều kiện Voucher (ví dụ: Voucher GOLD chỉ cho hạng Vàng trở lên đổi).
* **Quy tắc giảm giá tự động:** Khi đặt lịch trực tuyến hoặc tại quầy, hệ thống tự động nhận diện hạng thành viên để áp dụng mức chiết khấu giảm giá trực tiếp (MEMBER: 0%, SILVER: 2%, GOLD: 5%, PLATINUM: 10% trên tổng hóa đơn).

### ⚙️ Phân hệ 04: Cấu hình ngày nghỉ (Day Off) & Cảnh báo xung đột
* **Cơ chế hoạt động:** Admin thiết lập cấu hình hoạt động cho ngày mai (Đổi giờ mở cửa, số khoang rửa, ca hoạt động hoặc đánh dấu Đóng cửa ngày nghỉ).
* **Ràng buộc kiểm soát xung đột:**
  * Nếu Admin lưu cấu hình ngày nghỉ mà ngày đó **đã có khách hàng đặt lịch trước** $\rightarrow$ Hệ thống sẽ trả về mã lỗi `409 CONFLICT` kèm danh sách mã đơn bị ảnh hưởng $\rightarrow$ Frontend hiển thị Modal cảnh báo.
  * Nếu Admin chấp nhận ghi đè (`forceSave = true`) $\rightarrow$ Hệ thống thực hiện lưu cấu hình ngày nghỉ, đồng thời tự động hủy toàn bộ các đơn hàng bị xung đột và hoàn cọc 100% về ví cho các khách hàng này.

### 🏬 Phân hệ 05: Quản lý ma trận giá dịch vụ theo size xe
* **Quy tắc định giá:** Một gói dịch vụ (ví dụ: Rửa xe bọt tuyết) sẽ có mức giá và thời gian thi công khác nhau dựa trên kích thước của xe (Nhỏ: `SMALL`, Vừa: `MEDIUM`, Lớn: `LARGE`).
* **Hoạt động của Frontend:** Khi khách hàng hoặc Staff Counter chọn xe $\rightarrow$ Hệ thống tự động nhận diện size xe và hiển thị chính xác bảng giá, thời gian thi công tương ứng của dịch vụ.

### 📅 Phân hệ 06: Quản lý đặt lịch, check-in QR & yêu cầu hủy (Mới hoàn thiện)
* **Tính toán khung giờ trống động (Slot Availability):** 
  * Khách hàng đặt lịch chọn ngày $\rightarrow$ Hệ thống tính toán thời gian trống động bằng cách kiểm tra số khoang rửa tại cửa hàng so với số xe đang rửa trong cùng khung giờ, cộng thêm thời gian đệm bảo trì giữa các ca để đưa ra các khung giờ trống khả dụng thực tế.
* **Check-in QR Code:** Khách hàng đến cửa hàng đưa mã QR $\rightarrow$ Staff quét mã $\rightarrow$ Hệ thống kiểm tra đi trễ quá 15 phút hay đúng giờ để tự động Check-in (chuyển đơn sang trạng thái `ARRIVED` đưa vào hàng chờ).
* **Yêu cầu hủy từ Staff:**
  * Nhân viên tại tiệm phát hiện xe gặp sự cố hoặc thiết bị lỗi $\rightarrow$ Gửi đề xuất hủy kèm lý do lên hệ thống $\rightarrow$ Đơn hàng đổi trạng thái yêu cầu thành `PENDING`.
  * **Chặn thao tác sai:** Khi đơn hàng ở trạng thái chờ duyệt hủy `PENDING`, nhân viên sẽ bị hệ thống **chặn hoàn toàn** quyền check-in hoặc điều phối xe vào khoang rửa để tránh thao tác lỗi.
  * **Admin xử lý:** Admin xem danh sách chờ duyệt hủy, có quyền **Duyệt hủy** (đơn hủy, tự động hoàn tiền ví 100%) hoặc **Từ chối hủy** (đơn hàng khôi phục hoạt động bình thường, lưu lại phản hồi bác bỏ của Admin).
  * **Khóa đơn hoàn thành:** Đơn hàng đã ở trạng thái `COMPLETED` sẽ bị **khóa cứng** (không thể bấm Sửa trạng thái hay xóa) để bảo toàn dữ liệu doanh thu và lịch sử ví.

### 👥 Phân hệ 07: Quản trị tài khoản & điểm thưởng
* **Quyền hạn Admin:** CRUD tài khoản nhân viên (Staff), khóa/mở khóa tài khoản khách hàng, và đặc biệt là quyền điều chỉnh điểm thưởng trực tiếp cho khách hàng. Hệ thống sẽ tự động tính toán lại phân hạng thành viên tương ứng ngay khi điểm thay đổi.

---

## 🧪 3. HƯỚNG DẪN TEST NHANH CÁC NGHIỆP VỤ

### Kịch bản 1: Test luồng Staff gửi yêu cầu hủy $\rightarrow$ Admin duyệt hủy
1. Chạy file SQL [seed_cancellation_test_data.sql](file:///C:/Users/HP/github/SWP301/AutowashProject/backend/seed_cancellation_test_data.sql) để tạo đơn kiểm thử.
2. Đăng nhập tài khoản Nhân viên (`staff01` / `123456`).
3. Tạo lịch đặt vãng lai hoặc tìm đơn `BK-CANCEL-PENDING` $\rightarrow$ Thấy trạng thái chờ duyệt hủy. Thử bấm Check-in QR $\rightarrow$ Hệ thống báo lỗi chặn thành công.
4. Đăng nhập tài khoản Admin (`admin01` / `123456`).
5. Vào **Quản lý đơn đặt lịch** $\rightarrow$ Thấy đơn `BK-CANCEL-PENDING` hiển thị dòng chữ màu vàng *"Chờ duyệt hủy"* xếp chồng cực kỳ ngay ngắn và không bị tràn cột Tổng tiền.
6. Bấm nút **Duyệt hủy** (icon check màu xanh) $\rightarrow$ Đơn hàng chuyển sang `CANCELLED`, trạng thái yêu cầu thành `APPROVED`.
7. Kiểm tra Ví của khách hàng (User 4) $\rightarrow$ Thấy số dư được hoàn trả chính xác 100,000đ.

### Kịch bản 2: Test cấu hình ngày nghỉ và xử lý xung đột hủy đơn
1. Đăng nhập tài khoản Admin (`admin01` / `123456`).
2. Vào **Cấu hình ngày nghỉ** $\rightarrow$ Đặt ngày nghỉ cho ngày mai.
3. Bấm **Lưu cấu hình** $\rightarrow$ Hệ thống phát hiện ngày mai có đơn `BK-CANCEL-PENDING` $\rightarrow$ Hiển thị Warning Modal thông báo có xung đột lịch đặt.
4. Bấm **Tiếp tục ghi đè & hủy đơn** $\rightarrow$ Đơn đặt lịch ngày mai tự động chuyển sang `CANCELLED` và hoàn tiền về ví cho khách, hệ thống áp dụng ngày nghỉ lễ thành công.
