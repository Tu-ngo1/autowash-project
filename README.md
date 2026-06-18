# AutowashProject

hoc Git di nhe, nho dung dung branch cua minh

BE 2 - Service, Price, Booking, QR, Staff Flow
Phu trach phan van hanh rua xe.

Files can lam:

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

5/Chuc nang:
- Admin tao/sua/xoa dich vu
- Admin tao/sua/xoa gia dich vu theo kich co xe
- Customer xem dich vu va gia
- Customer xem slot trong
- Customer dat lich
- Validate trung slot
- Validate thoi gian dat lich
- Validate booking window theo tier
- Sinh QR cho booking
- Customer xem booking cua minh
- Customer huy booking
- Staff quet QR/check-in
- Staff chuyen trang thai:
  PENDING -> ARRIVED -> IN_PROGRESS -> WASHED
