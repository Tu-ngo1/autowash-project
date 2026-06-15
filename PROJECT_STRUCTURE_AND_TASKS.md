# Autowash Backend - Cấu trúc dự án và nhiệm vụ cần làm

Tài liệu này dùng để ghi nhớ cấu trúc backend Spring Boot của dự án Autowash và các việc cần làm khi phát triển chức năng mới.

## 1. Luồng xử lý chính

Luồng chuẩn của project:

```text
USER / FRONTEND
 -> CONTROLLER
 -> SERVICE
 -> REPOSITORY
 -> JPA / HIBERNATE
 -> DATABASE TABLE
```

Ý nghĩa từng tầng:

```text
Controller
```

Nhận request từ frontend hoặc người dùng, lấy path variable, query param, request body nếu có, sau đó gọi service.

```text
Service
```

Chứa business logic. Service quyết định gọi repository nào, xử lý dữ liệu, kiểm tra điều kiện và trả DTO response.

```text
Repository
```

Làm việc với database thông qua Spring Data JPA. Repository thường là interface extends `JpaRepository`.

```text
Entity
```

Đại diện cho table trong database. Entity có `@Entity`, `@Table`, `@Column`, `@ManyToOne`, `@OneToOne`, ...

```text
DTO
```

Object dùng để nhận request hoặc trả response. DTO không phải table trong database.

```text
Infrastructure
```

Chứa phần hạ tầng kỹ thuật như security, JWT, config, filter, tích hợp bên ngoài. Không phải nơi chính để viết CRUD database.

## 2. Cấu trúc package hiện tại

```text
src/main/java/com/autowash
 ├── controller
 ├── dto
 │   ├── request
 │   └── response
 ├── entity
 ├── enums
 ├── infrastructure
 │   ├── config
 │   └── security
 ├── repository
 └── service
```

## 3. Vai trò từng package

### `controller`

Chứa REST API controller.

Ví dụ:

```text
AuthController
AdminController
CustomerController
StaffController
```

Nhiệm vụ:

- Khai báo endpoint bằng `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`.
- Nhận dữ liệu từ request.
- Gọi service tương ứng.
- Trả response cho frontend.

Controller không nên viết query database trực tiếp.

### `service`

Chứa logic xử lý chính.

Ví dụ:

```text
AuthService
UserService
AnalyticsService
```

Nhiệm vụ:

- Gọi repository để lấy dữ liệu.
- Kiểm tra rule nghiệp vụ.
- Xử lý dữ liệu trước khi trả về.
- Ném exception hợp lý nếu dữ liệu không hợp lệ.

Service không nên biết chi tiết HTTP request quá nhiều. Phần HTTP nên nằm ở controller.

### `repository`

Chứa các interface thao tác database.

Ví dụ:

```text
UserRepository
BookingRepository
PaymentRepository
TierConfigRepository
```

Repository thường viết:

```java
public interface BookingRepository extends JpaRepository<Booking, Long> {
}
```

Nhiệm vụ:

- Dùng method có sẵn của `JpaRepository`: `findAll`, `findById`, `save`, `deleteById`, ...
- Viết derived query method nếu đơn giản.
- Viết `@Query` nếu cần query phức tạp, thống kê, `JOIN`, `GROUP BY`.

### `entity`

Chứa các class map với database table.

Ví dụ:

```text
Booking
Payment
CustomerVoucher
Promotion
User
Car
Review
Service
ServicePrice
```

Nhiệm vụ:

- Khai báo table bằng `@Table`.
- Khai báo column bằng `@Column`.
- Khai báo quan hệ bằng `@ManyToOne`, `@OneToOne`, `@OneToMany`.

Ví dụ:

```java
@OneToOne
@JoinColumn(name = "applied_voucher_id")
private CustomerVoucher appliedVoucher;
```

Mapping này giúp JPQL tự hiểu join theo column nào.

### `dto/request`

Chứa object nhận dữ liệu từ request.

Ví dụ:

```text
LoginRequest
RegisterRequest
UpdateProfileRequest
```

### `dto/response`

Chứa object trả dữ liệu cho frontend.

Ví dụ:

```text
AuthResponse
UserResponse
ProfileResponse
BookingStatusCountResponse
TopUsedVoucherResponse
```

DTO response thường dùng khi không muốn trả thẳng entity ra frontend.

### `enums`

Chứa các enum trạng thái hoặc loại dữ liệu cố định.

Ví dụ:

```text
Role
BookingStatus
PaymentStatus
VoucherStatus
UserStatus
PaymentMethod
VehicleSize
TierLevel
```

### `infrastructure`

Chứa phần kỹ thuật nền của hệ thống.

Hiện tại có:

```text
SecurityConfig
JwtAuthenticationFilter
JwtService
```

Nhiệm vụ:

- Cấu hình security.
- Kiểm tra JWT token.
- Generate và validate JWT.
- Cấu hình các thành phần kỹ thuật khác nếu có.

Không nên đặt CRUD repository trong package này.

## 4. Quy tắc đặt tên

Class, interface, enum dùng PascalCase:

```text
BookingRepository
AnalyticsService
TopUsedVoucherResponse
BookingStatus
```

Method, variable, field dùng camelCase:

```text
countBookingsByStatus
findTopUsedVouchers
bookingRepository
paymentStatus
```

Package viết thường:

```text
com.autowash.repository
com.autowash.dto.response
```

Constant dùng UPPER_SNAKE_CASE:

```text
PAID
PENDING
COMPLETED
```

## 5. Cách làm một chức năng mới

Khi nhận một requirement mới, làm theo checklist:

1. Xác định chức năng thuộc role nào: admin, customer, staff hay public.
2. Xác định endpoint cần tạo trong controller nào.
3. Xác định request cần input gì không.
4. Xác định response cần trả những field nào.
5. Tạo request DTO nếu API cần body input.
6. Tạo response DTO nếu API trả dữ liệu custom.
7. Tạo hoặc sửa repository để query database.
8. Tạo hoặc sửa service để xử lý logic.
9. Tạo hoặc sửa controller để expose API.
10. Kiểm tra security path trong `SecurityConfig`.
11. Chạy test hoặc chạy app để kiểm tra lỗi compile.
12. Test API bằng Postman, Swagger hoặc frontend.

## 6. Analytics hiện tại

### FR-033: Admin xem voucher được dùng nhiều nhất

Mục tiêu:

```text
Admin xem promotion/voucher nào được sử dụng nhiều nhất trong các payment đã PAID.
```

Các bảng/entity liên quan:

```text
Payment
CustomerVoucher
Promotion
```

Quan hệ:

```text
Payment.appliedVoucher -> CustomerVoucher
CustomerVoucher.promotion -> Promotion
```

Luồng xử lý:

```text
AdminController
 -> AnalyticsService
 -> PaymentRepository
 -> Payment / CustomerVoucher / Promotion
```

Files liên quan:

```text
src/main/java/com/autowash/controller/AdminController.java
src/main/java/com/autowash/service/AnalyticsService.java
src/main/java/com/autowash/repository/PaymentRepository.java
src/main/java/com/autowash/dto/response/TopUsedVoucherResponse.java
```

Query JPQL nên dùng:

```java
@Query("""
    SELECT new com.autowash.dto.response.TopUsedVoucherResponse(
        promo.id,
        promo.voucherCode,
        promo.campaignName,
        COUNT(payment)
    )
    FROM Payment payment
    JOIN payment.appliedVoucher customerVoucher
    JOIN customerVoucher.promotion promo
    WHERE payment.paymentStatus = com.autowash.enums.PaymentStatus.PAID
    GROUP BY promo.id, promo.voucherCode, promo.campaignName
    ORDER BY COUNT(payment) DESC
""")
List<TopUsedVoucherResponse> findTopUsedVouchers();
```

Giải thích:

```text
FROM Payment payment
```

Bắt đầu từ entity `Payment`, đặt alias là `payment`.

```text
JOIN payment.appliedVoucher customerVoucher
```

Join từ `Payment` sang field `appliedVoucher`, đặt alias mới là `customerVoucher`.

```text
JOIN customerVoucher.promotion promo
```

Join từ `CustomerVoucher` sang field `promotion`, đặt alias mới là `promo`.

```text
WHERE payment.paymentStatus = PAID
```

Chỉ tính các payment đã thanh toán thành công.

```text
GROUP BY promo.id, promo.voucherCode, promo.campaignName
```

Gom nhóm theo promotion/voucher.

```text
COUNT(payment)
```

Đếm số lần promotion đó được dùng.

### FR-034: Admin xem số lượng booking theo trạng thái

Mục tiêu:

```text
Admin xem mỗi trạng thái booking có bao nhiêu booking.
```

Entity liên quan:

```text
Booking
```

Field quan trọng:

```java
private BookingStatus status;
```

Luồng xử lý:

```text
AdminController
 -> AnalyticsService
 -> BookingRepository
 -> Booking
```

Files liên quan:

```text
src/main/java/com/autowash/controller/AdminController.java
src/main/java/com/autowash/service/AnalyticsService.java
src/main/java/com/autowash/repository/BookingRepository.java
src/main/java/com/autowash/dto/response/BookingStatusCountResponse.java
```

Query JPQL nên dùng:

```java
@Query("""
    SELECT new com.autowash.dto.response.BookingStatusCountResponse(
        b.status,
        COUNT(b)
    )
    FROM Booking b
    GROUP BY b.status
""")
List<BookingStatusCountResponse> countBookingsByStatus();
```

DTO phải có constructor tương ứng:

```java
public BookingStatusCountResponse(BookingStatus status, Long total)
```

Nếu dùng Lombok:

```java
@Getter
@AllArgsConstructor
public class BookingStatusCountResponse {
    private BookingStatus status;
    private Long total;
}
```

## 7. Nhiệm vụ cần làm cho Analytics

### Repository

- Hoàn thiện `BookingRepository`.
- Hoàn thiện `PaymentRepository`.
- Đảm bảo query JPQL đúng tên entity, field, DTO class.
- Đảm bảo các DTO có constructor đúng với `SELECT new ...`.

### DTO

- Sửa `BookingStatusCountResponse`:
  - `status` phải là `BookingStatus`, không phải `boolean`.
  - `total` phải là `Long`, không phải `int`.
  - Có getter.
  - Có constructor đủ tham số.

- Sửa hoặc tạo `TopUsedVoucherResponse`:
  - Có field thông tin promotion/voucher.
  - Có `usedCount` kiểu `Long`.
  - Có getter.
  - Có constructor đủ tham số.

### Service

- Tạo hoặc hoàn thiện `AnalyticsService`.
- Inject `BookingRepository`.
- Inject `PaymentRepository`.
- Tạo method:

```java
public List<BookingStatusCountResponse> getBookingsByStatus()
```

- Tạo method:

```java
public List<TopUsedVoucherResponse> getTopUsedVouchers()
```

### Controller

- Sửa `AdminController`.
- Thêm base path:

```java
@RequestMapping("/api/admin")
```

- Inject `AnalyticsService`.
- Tạo endpoint:

```java
GET /api/admin/analytics/bookings-by-status
```

- Tạo endpoint:

```java
GET /api/admin/analytics/top-used-vouchers
```

## 8. Những nhiệm vụ backend lớn còn lại

Dựa trên entity hiện tại, các nhóm chức năng backend có thể cần hoàn thiện gồm:

### Authentication và Authorization

- Đăng ký.
- Đăng nhập.
- JWT authentication.
- Phân quyền theo role: `ADMIN`, `CUSTOMER`, `STAFF`.
- Chặn endpoint theo role trong `SecurityConfig`.

### User và Profile

- Xem thông tin user hiện tại.
- Cập nhật profile.
- Quản lý customer profile.
- Quản lý điểm thưởng và tier.

### Car

- Customer thêm xe.
- Customer xem danh sách xe.
- Customer sửa thông tin xe.
- Customer xóa xe nếu hợp lệ.

### Service và Service Price

- Admin quản lý dịch vụ rửa xe.
- Admin quản lý giá theo size xe.
- Customer xem danh sách dịch vụ.

### Booking

- Customer tạo booking.
- Customer xem booking của mình.
- Staff xác nhận hoặc cập nhật trạng thái booking.
- Admin xem toàn bộ booking.
- Validate thời gian booking.
- Validate xe, dịch vụ và user.

### Payment

- Tạo payment cho booking.
- Cập nhật trạng thái payment.
- Áp dụng voucher vào payment.
- Tính subtotal, discount amount, final price.
- Chỉ tính voucher analytics khi payment `PAID`.

### Promotion và Voucher

- Admin tạo promotion.
- Admin cập nhật promotion.
- Customer đổi điểm lấy voucher.
- Customer xem voucher của mình.
- Cập nhật trạng thái voucher: `AVAILABLE`, `USED`, `EXPIRED`, `CANCELLED`.

### Review

- Customer đánh giá booking đã hoàn thành.
- Admin/staff xem review.
- Chặn review trùng booking.

### Analytics

- FR-033: Voucher được dùng nhiều nhất.
- FR-034: Số lượng booking theo trạng thái.
- Có thể mở rộng thêm:
  - Doanh thu theo ngày/tháng.
  - Booking theo ngày/tháng.
  - Top dịch vụ được đặt nhiều nhất.
  - Tỷ lệ booking bị hủy.
  - Số lượng customer mới.

## 9. Checklist trước khi hoàn thành một API

- Endpoint có đúng path chưa.
- Endpoint có đúng role chưa.
- Controller chỉ gọi service, không viết query.
- Service chứa logic chính.
- Repository chỉ query database.
- DTO response không trả dư thông tin nhạy cảm.
- Query JPQL dùng đúng tên entity và field.
- `SELECT new ...` dùng đúng full package DTO.
- DTO có constructor đúng kiểu dữ liệu.
- `COUNT(...)` dùng `Long`.
- App compile được.
- API test được với token đúng role.

## 10. Ghi nhớ về JPQL

JPQL dùng tên entity và field Java, không dùng trực tiếp tên table và column.

Ví dụ đúng:

```java
FROM Booking b
GROUP BY b.status
```

Không viết theo table:

```sql
FROM BOOKINGS
GROUP BY status
```

Join trong JPQL đi theo field quan hệ:

```java
JOIN payment.appliedVoucher customerVoucher
JOIN customerVoucher.promotion promo
```

Hibernate tự hiểu column join dựa trên annotation:

```java
@JoinColumn(name = "applied_voucher_id")
@JoinColumn(name = "promotion_id")
```

Keyword JPQL như `SELECT`, `FROM`, `JOIN`, `WHERE` không quan trọng hoa thường, nhưng tên class, field, DTO, enum phải đúng hoa thường.

