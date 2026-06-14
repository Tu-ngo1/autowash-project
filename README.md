# AutowashProject
<<<<<<< Updated upstream
6/6/2026 push phần BE2 lên lần 1
=======
BE 2 - Service, Price, Booking, QR, Staff Flow
Phụ trách phần vận hành rửa xe.

Files cần làm:

1/repository/
- ServiceRepository
- ServicePriceRepository
- BookingRepository
- BookingDetailRepository

2/dto/request/
- CreateServiceRequest
- UpdateServiceRequest
- CreateServicePriceRequest
- UpdateServicePriceRequest
- CreateBookingRequest
- UpdateBookingStatusRequest

3/dto/response/
- ServiceResponse
- ServicePriceResponse
- BookingResponse
- BookingDetailResponse
- AvailableSlotResponse
- QrCodeResponse

4/services/
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

5/Chức năng:
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
>>>>>>> Stashed changes
