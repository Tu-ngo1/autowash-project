BE 1 - Auth, User, Profile, Car
Phụ trách phần nền tảng người dùng.

Files cần làm:

repository/
- UserRepository
- CustomerProfileRepository
- OtpTokenRepository
- CarRepository
dto/request/
- RegisterRequest
- LoginRequest
- UpdateProfileRequest
- CreateCarRequest
- UpdateCarRequest
dto/response/
- AuthResponse
- UserResponse
- ProfileResponse
- CarResponse
services/
- AuthService
- UserService
- OtpService
- CarService
controller/
- AuthController
- UserController
- CarController
infrastructure/
- SecurityConfig
- JwtService
- JwtAuthenticationFilter
- CustomUserDetailsService
- PasswordEncoderConfig
Chức năng:

- Đăng ký Customer
- Đăng nhập
- JWT authentication
- Phân quyền CUSTOMER / STAFF / ADMIN
- Xem thông tin user hiện tại
- Customer cập nhật profile
- Admin khóa/mở khóa user
- Admin tạo tài khoản Staff
- Customer thêm/sửa/xóa xe
BE 2 - Service, Price, Booking, QR, Staff Flow
Phụ trách phần vận hành rửa xe.

Files cần làm:

repository/
- ServiceRepository
- ServicePriceRepository
- BookingRepository
- BookingDetailRepository
dto/request/
- CreateServiceRequest
- UpdateServiceRequest
- CreateServicePriceRequest
- UpdateServicePriceRequest
- CreateBookingRequest
- UpdateBookingStatusRequest
dto/response/
- ServiceResponse
- ServicePriceResponse
- BookingResponse
- BookingDetailResponse
- AvailableSlotResponse
- QrCodeResponse
services/
- WashService
- ServicePriceService
- BookingService
- BookingDetailService
- QrCodeService
controller/
- ServiceController
- ServicePriceController
- BookingController
- StaffBookingController
Chức năng:

- Admin tạo/sửa/xóa dịch vụ
- Admin tạo/sửa/xóa giá dịch vụ theo kích cỡ xe
- Customer xem dịch vụ và giá
- Customer xem slot trống
- Customer đặt lịch
- Validate trùng slot
- Validate thời gian đặt lịch
- Validate booking window theo tier
- Sinh QR cho booking
- Customer xem booking của mình
- Customer hủy booking
- Staff quét QR/check-in
- Staff chuyển trạng thái:
  PENDING -> ARRIVED -> IN_PROGRESS -> WASHED
BE 3 - Payment, Loyalty, Voucher, Review, Analytics
Phụ trách phần tiền, điểm, khuyến mãi và báo cáo.

Files cần làm:

repository/
- PaymentRepository
- TierConfigRepository
- PromotionRepository
- CustomerVoucherRepository
- ReviewRepository
dto/request/
- CreatePaymentRequest
- ApplyVoucherRequest
- CreatePromotionRequest
- UpdatePromotionRequest
- RedeemVoucherRequest
- CreateReviewRequest
dto/response/
- PaymentResponse
- LoyaltyResponse
- TierConfigResponse
- PromotionResponse
- CustomerVoucherResponse
- ReviewResponse
- RevenueAnalyticsResponse
- TopVoucherResponse
services/
- PaymentService
- LoyaltyService
- TierConfigService
- PromotionService
- CustomerVoucherService
- ReviewService
- AnalyticsService
controller/
- PaymentController
- LoyaltyController
- PromotionController
- CustomerVoucherController
- ReviewController
- AnalyticsController
infrastructure/
- LoyaltyScheduler
- SchedulerConfig
Chức năng:

- Staff chốt thanh toán booking
- Tạo payment
- Áp dụng voucher vào payment
- Validate voucher thuộc đúng customer
- Validate voucher chưa dùng/chưa hết hạn
- Chuyển booking WASHED -> COMPLETED sau khi thanh toán
- Cộng reward_points
- Cộng tier_points
- Tự động nâng hạng customer
- Scheduled task kiểm tra giảm hạng/hết hạn điểm nếu làm
- Admin tạo/sửa/xóa promotion
- Customer đổi điểm lấy voucher
- Customer xem voucher của mình
- Customer đánh giá booking đã hoàn thành
- Admin xem doanh thu
- Admin xem voucher dùng nhiều nhất
- Admin xem số lượng booking theo ngày/tháng
BE 1 làm Auth + Security trước, vì BE 2 và BE 3 cần user đăng nhập và role.
BE 2 làm Service + ServicePrice, rồi mới làm Booking.
BE 3 làm TierConfig + Promotion trước, rồi Payment + Loyalty.
Sau khi BE 2 có booking status WASHED, BE 3 nối tiếp để làm thanh toán và chuyển COMPLETED.
Ranh Giới Quan Trọng

BE 1 quản lý User, CustomerProfile, Car.
BE 2 quản lý Service, ServicePrice, Booking, BookingDetail.
BE 3 quản lý Payment, TierConfig, Promotion, CustomerVoucher, Review, Analytics.
Khi code, mỗi BE nên tự làm đủ:

Repository -> DTO -> Service -> Controller
