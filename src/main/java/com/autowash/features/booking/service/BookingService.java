package com.autowash.features.booking.service;

import com.autowash.features.washservice.service.WashService;

import com.autowash.features.booking.dto.request.CreateBookingRequest;
import com.autowash.features.booking.dto.request.UpdateBookingStatusRequest;
import com.autowash.features.booking.dto.response.BookingDetailResponse;
import com.autowash.features.booking.dto.response.BookingResponse;
import com.autowash.features.booking.dto.response.QrCodeResponse;
import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.entity.BookingDetail;
import com.autowash.features.car.entity.Car;
import com.autowash.features.user.entity.CustomerProfile;
import com.autowash.features.washservice.entity.ServicePrice;
import com.autowash.features.user.entity.User;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.car.enums.CarStatus;
import com.autowash.features.booking.repository.BookingDetailRepository;
import com.autowash.features.booking.mapper.BookingMapper;
import com.autowash.features.booking.repository.BookingRepository;
import com.autowash.features.car.repository.CarRepository;
import com.autowash.features.washservice.repository.ServicePriceRepository;
import com.autowash.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import com.autowash.features.booking.repository.DailyOperationsConfigRepository;
import com.autowash.features.booking.dto.response.AvailableSlotResponse;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final UserRepository userRepository;
    private final CarRepository carRepository;
    private final BookingMapper bookingMapper;
    private final DailyOperationsConfigRepository dailyOperationsConfigRepository;
    private final QrCodeService qrCodeService;

    private static final int MIN_BOOKING_BUFFER_MINUTES = 30;
    private static final int CANCEL_BUFFER_MINUTES = 60;

    public BookingResponse createBooking(Long customerId, CreateBookingRequest request) {

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy khách hàng"
                ));

        Car car = carRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy phương tiện"
                ));

        if (!car.getUser().getId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền đặt lịch cho phương tiện này"
            );
        }

        if (car.getStatus() != CarStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phương tiện đã bị xóa hoặc không hoạt động"
            );
        }

        validateBookingTime(request.getScheduledStartTime());
        validateVehicleHasNoActiveBooking(car.getId());
        validateBookingWindow(customer, request.getScheduledStartTime());
        validateSlotAvailable(request.getScheduledStartTime());

        List<ServicePrice> selectedPrices = request.getServiceIds()
                .stream()
                .map(serviceId -> servicePriceRepository
                        .findByServiceIdAndVehicleSizeAndActiveTrue(
                                serviceId,
                                car.getVehicleModel().getVehicleSize()
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Không tìm thấy giá dịch vụ phù hợp với kích cỡ xe"
                        )))
                .toList();

        int totalPrice = selectedPrices.stream()
                .mapToInt(ServicePrice::getPrice)
                .sum();

        int totalDuration = selectedPrices.stream()
                .mapToInt(ServicePrice::getDurationMinutes)
                .sum();

        LocalDateTime expectedEndTime =
                request.getScheduledStartTime().plusMinutes(totalDuration);


        String bookingCode = generateBookingCode();
        String qrContent = qrCodeService.generateQrContent(bookingCode);

        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .user(customer)
                .vehicle(car)
                .scheduledStartTime(request.getScheduledStartTime())
                .expectedEndTime(expectedEndTime)
                .status(BookingStatus.PENDING)
                .customerNote(request.getCustomerNote())
                .totalPrice(totalPrice)
                .qrContent(qrContent)
                .qrUsed(false)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        List<BookingDetail> details = selectedPrices.stream()
                .map(price -> BookingDetail.builder()
                        .booking(savedBooking)
                        .servicePrice(price)
                        .actualPrice(price.getPrice())
                        .actualDurationMinutes(price.getDurationMinutes())
                        .build())
                .toList();

        bookingDetailRepository.saveAll(details);

        return bookingMapper.toResponse(savedBooking);
    }

    public List<BookingResponse> getMyBookings(Long customerId) {
        return bookingRepository.findByUserIdOrderByScheduledStartTimeDesc(customerId)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    public BookingResponse getBookingById(Long bookingId) {
        return bookingMapper.toResponse(findBookingOrThrow(bookingId));
    }

    public void cancelBooking(Long customerId, Long bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (!booking.getUser().getId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền hủy booking này"
            );
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ có thể hủy booking ở trạng thái PENDING"
            );
        }

        LocalDateTime latestCancelTime =
                booking.getScheduledStartTime().minusMinutes(CANCEL_BUFFER_MINUTES);

        if (LocalDateTime.now().isAfter(latestCancelTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bạn chỉ được hủy lịch trước ít nhất 60 phút"
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    public BookingResponse checkInByQr(String qrContent) {
        Booking booking = bookingRepository.findByQrContent(qrContent)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Mã QR không hợp lệ"
                ));

        if (Boolean.TRUE.equals(booking.getQrUsed())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "QR đã được sử dụng"
            );
        }

        booking.setQrUsed(true);
        booking.setStatus(BookingStatus.ARRIVED);
        booking.setArrivedAt(LocalDateTime.now());

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    public BookingResponse updateBookingStatus(
            Long bookingId,
            UpdateBookingStatusRequest request
    ) {
        Booking booking = findBookingOrThrow(bookingId);

        BookingStatus currentStatus = booking.getStatus();
        BookingStatus nextStatus = request.getStatus();

        validateStatusFlow(currentStatus, nextStatus);

        booking.setStatus(nextStatus);

        if (nextStatus == BookingStatus.ARRIVED) {
            booking.setArrivedAt(LocalDateTime.now());
        }

        if (nextStatus == BookingStatus.IN_PROGRESS) {
            booking.setWashStartedAt(LocalDateTime.now());
        }

        if (nextStatus == BookingStatus.WASHED) {
            booking.setCompletedAt(LocalDateTime.now());
        }

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    public QrCodeResponse getQrCode(Long customerId, Long bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (!booking.getUser().getId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền xem QR của booking này"
            );
        }

        return QrCodeResponse.builder()
                .bookingCode(booking.getBookingCode())
                .qrContent(booking.getQrContent())
                .build();
    }

    private void validateBookingTime(LocalDateTime scheduledStartTime) {
        LocalDateTime earliestAllowedTime =
                LocalDateTime.now().plusMinutes(MIN_BOOKING_BUFFER_MINUTES);

        if (scheduledStartTime.isBefore(earliestAllowedTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking phải được tạo trước ít nhất 30 phút"
            );
        }
    }

    private void validateVehicleHasNoActiveBooking(Long vehicleId) {
        Collection<BookingStatus> activeStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.ARRIVED,
                BookingStatus.IN_PROGRESS
        );

        boolean exists = bookingRepository.existsByVehicleIdAndStatusIn(
                vehicleId,
                activeStatuses
        );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phương tiện đang có booking chưa hoàn thành"
            );
        }
    }


    private void validateSlotAvailable(LocalDateTime scheduledStartTime) {
        Collection<BookingStatus> activeStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.ARRIVED,
                BookingStatus.IN_PROGRESS
        );

        boolean exists = bookingRepository.existsByScheduledStartTimeAndStatusIn(
                scheduledStartTime,
                activeStatuses
        );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Slot này đã có booking khác"
            );
        }
    }

    private void validateBookingWindow(User customer, LocalDateTime scheduledStartTime) {
        int bookingWindowDays = getBookingWindowByTier(customer);

        LocalDateTime maxAllowedTime =
                LocalDateTime.now().plusDays(bookingWindowDays);

        if (scheduledStartTime.isAfter(maxAllowedTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hạng thành viên hiện tại chỉ được đặt lịch trong "
                            + bookingWindowDays + " ngày tới"
            );
        }
    }

    private int getBookingWindowByTier(User customer) {
        CustomerProfile profile = customer.getCustomerProfile();

        if (profile == null || profile.getTierConfig() == null) {
            return 7;
        }

        return profile.getTierConfig().getBookingWindowDays();
    }

    private void validateStatusFlow(
            BookingStatus currentStatus,
            BookingStatus nextStatus
    ) {
        boolean valid =
                (currentStatus == BookingStatus.PENDING
                        && nextStatus == BookingStatus.ARRIVED)
                        || (currentStatus == BookingStatus.ARRIVED
                        && nextStatus == BookingStatus.IN_PROGRESS)
                        || (currentStatus == BookingStatus.IN_PROGRESS
                        && nextStatus == BookingStatus.WASHED);

        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể chuyển trạng thái từ "
                            + currentStatus + " sang " + nextStatus
            );
        }
    }

    private Booking findBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy booking"
                ));
    }

    private String generateBookingCode() {
        return "BK-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }

    private String generateQrContent() {
        return "QR-" + UUID.randomUUID();
    }

    public List<AvailableSlotResponse> getAvailableSlots(LocalDate date) {
        // TODO: USER sẽ tự tay viết logic tính toán các slot đặt lịch trống tại đây để hiểu rõ cơ chế hoạt động.
        // Gợi ý: 
        // 1. Lấy cấu hình từ dailyOperationsConfigRepository theo ngày (hoặc dùng mặc định: 8h - 18h, số khoang = 2, interval = 90 phút).
        // 2. Lặp qua các mốc thời gian bắt đầu và đếm số lượng xe đang rửa tại mỗi mốc.
        // 3. Nếu số xe < số khoang rửa và thời gian đặt lịch thỏa mãn (ví dụ: > hiện tại + 30 phút), đánh dấu ca đó khả dụng.
        return new ArrayList<>();
    }
}

