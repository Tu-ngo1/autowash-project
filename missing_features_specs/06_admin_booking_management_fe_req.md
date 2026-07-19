# Yêu cầu Phát triển Frontend - Tính Năng Yêu Cầu Hủy & Duyệt Hủy Lịch Đặt Xe

Tài liệu này đặc tả chi tiết các màn hình, component, tích hợp API và logic nghiệp vụ cần triển khai ở phía Frontend (FE) để hỗ trợ luồng: **Staff yêu cầu hủy đơn -> Admin duyệt hủy / bác bỏ** và **Admin hủy trực tiếp**.

---

## 1. Tích hợp API mới (Services)

Cần bổ sung các hàm gọi API vào các file service hiện tại:

### 1.1. Service của Staff (`frontend/src/services/staffBookingApi.js`)
Thêm hàm gửi yêu cầu hủy lịch:
```javascript
// Gửi yêu cầu hủy lịch kèm lý do (POST)
export const requestCancelBooking = (id, reason) =>
  api.post(apiPath(`/staff/bookings/${id}/cancel-request`), { reason });
```

### 1.2. Service của Admin (`frontend/src/services/adminBookingApi.js`)
Nâng cấp hàm lấy danh sách và thêm các hàm duyệt/bác bỏ:
```javascript
// 1. Lấy danh sách booking (hỗ trợ thêm query param cancelRequestStatus)
export const getAdminBookings = (params) =>
  api.get(apiPath("/admin/bookings"), { params });

// 2. Admin duyệt yêu cầu hủy từ Staff
export const approveCancelRequest = (id) =>
  api.post(apiPath(`/admin/bookings/${id}/cancel-request/approve`));

// 3. Admin bác bỏ yêu cầu hủy từ Staff
export const rejectCancelRequest = (id, adminNote) =>
  api.post(apiPath(`/admin/bookings/${id}/cancel-request/reject`), { adminNote });
```

---

## 2. Giao diện & Logic Nhân viên (Staff Dashboard)

Áp dụng tại: `StaffDashboard.jsx` và `StaffQueue.jsx`.

### 2.1. Nút "Yêu cầu hủy" (Request Cancel Button)
- **Vị trí hiển thị:** Trong Modal chi tiết lịch hẹn hoặc dòng Action của mỗi lịch hẹn ở trạng thái `PENDING`, `CONFIRM`, hoặc `ARRIVED`. Không hiển thị đối với lịch đã `IN_PROGRESS` (đang rửa) hoặc `COMPLETED`.
- **Logic hoạt động:**
  1. Khi nhân viên click vào nút **"Yêu cầu hủy"**, hệ thống sẽ mở ra một **Confirmation Modal**.
  2. Giao diện Modal bao gồm:
     - Tiêu đề: `Yêu cầu hủy lịch hẹn`
     - Cảnh báo: `Hành động này cần được Admin phê duyệt. Vui lòng nhập lý do hủy chi tiết.`
     - Ô nhập text (`textarea`): **Lý do hủy** (Bắt buộc). Có placeholder: `Nhập lý do hủy (ví dụ: Khoang chuyên sâu gặp sự cố kĩ thuật...)`.
     - Nút **"Gửi yêu cầu"**: Chỉ sáng lên và cho phép click khi ô nhập lý do có nội dung (không trống).
     - Nút **"Hủy bỏ"**: Đóng Modal.
  3. Khi click **"Gửi yêu cầu"**, gọi API `requestCancelBooking(bookingId, reason)`:
     - Nếu thành công: Hiển thị Toast thông báo thành công, đóng modal và tải lại (refresh) danh sách đặt lịch.
     - Nếu thất bại: Hiển thị thông báo lỗi tương ứng từ backend.

### 2.2. Hiển thị Badge trạng thái yêu cầu hủy
- Nếu một booking có `cancelRequestStatus === "PENDING"`, hiển thị thêm một Badge nổi bật: **"Chờ duyệt hủy"** (màu cam/vàng).
- **Chặn thao tác:** Chặn (Disable) hoàn toàn các nút hành động khác của Staff đối với đơn này như: *Xác nhận xe đến (Check-in)* hoặc *Điều phối vào khoang (Assign Bay)*.

---

## 3. Giao diện & Logic Quản trị viên (Admin Dashboard)

Áp dụng tại: `AdminBookings.jsx` và `AdminBookingsTable.jsx`.

### 3.1. Bộ lọc danh sách (Filter Bar)
- Thêm một bộ lọc hoặc Tab mới bên cạnh các trạng thái lịch đặt: **"Yêu cầu hủy chờ duyệt"** (Pending Cancel Requests).
- Khi chọn bộ lọc này, FE sẽ gọi API `getAdminBookings` kèm tham số query: `cancelRequestStatus: "PENDING"`.

### 3.2. Cải tiến bảng danh sách Booking (`AdminBookingsTable.jsx`)
- **Hiển thị Badge yêu cầu hủy:**
  - `PENDING`: Badge màu vàng/cam **"Chờ duyệt hủy"**.
  - `APPROVED`: Badge màu xanh lá **"Đã duyệt hủy"**.
  - `REJECTED`: Badge màu đỏ **"Bác bỏ yêu cầu hủy"**.
- **Hiển thị Thông tin lý do hủy:**
  - Trong bảng hoặc Modal chi tiết đơn đặt, nếu đơn có yêu cầu hủy, cần hiển thị rõ:
    - **Lý do hủy (Staff):** `cancelRequestReason`
    - **Người yêu cầu:** `cancelRequestedBy.fullName` (hoặc `username`)
    - **Thời gian yêu cầu:** `cancelRequestedAt` (định dạng ngày giờ thân thiện)
    - Nếu yêu cầu bị bác bỏ: Hiển thị thêm **Ghi chú của Admin:** `cancelRequestAdminNote`.

### 3.3. Các nút Hành động mới của Admin
Đối với các đơn hàng có `cancelRequestStatus === "PENDING"`, hiển thị 2 nút chức năng đặc quyền của Admin:

1. **Nút "Duyệt hủy" (Approve):**
   - Khi click, hiển thị Modal xác nhận: `Bạn có chắc chắn muốn duyệt yêu cầu hủy lịch đặt này? Khách hàng sẽ được hoàn lại 100% tiền vào ví.`
   - Khi Admin xác nhận, gọi API `approveCancelRequest(id)`.
   - Thành công: Hiện Toast thông báo, reload danh sách.

2. **Nút "Từ chối" / "Bác bỏ" (Reject):**
   - Khi click, hiển thị Modal:
     - Ô nhập phản hồi (`textarea`): **Ghi chú phản hồi cho Staff** (Không bắt buộc nhưng khuyến khích).
     - Nút **"Bác bỏ yêu cầu"**: Gọi API `rejectCancelRequest(id, adminNote)`.
     - Nút **"Hủy bỏ"**: Đóng Modal.
   - Thành công: Đơn hàng sẽ quay trở lại trạng thái vận hành trước đó, hiện Toast thông báo, reload danh sách.

### 3.4. Hủy đơn trực tiếp (Direct Cancellation)
- Nút **"Hủy đơn trực tiếp"** (hoặc icon xóa) của Admin vẫn khả dụng cho các đơn hoạt động thông thường (chưa hoàn thành và không ở trạng thái chờ duyệt hủy).
- Khi Admin chọn hủy đơn trực tiếp, hiển thị Modal cảnh báo: `Hủy trực tiếp đơn này đồng nghĩa hệ thống sẽ tự động hoàn trả 100% tiền cọc vào ví của khách hàng. Bạn có chắc chắn muốn tiếp tục?`.
- Xác nhận sẽ gọi API `deleteAdminBooking(id)`.
