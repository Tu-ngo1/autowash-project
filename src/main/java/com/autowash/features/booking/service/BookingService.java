package com.autowash.features.booking.service;

import com.autowash.features.washservice.service.WashService;
import com.autowash.features.booking.dto.request.CreateBookingRequest;
import com.autowash.features.booking.dto.request.UpdateBookingStatusRequest;
import com.autowash.features.booking.dto.response.BookingDetailResponse;
import com.autowash.features.booking.dto.response.BookingResponse;
import com.autowash.features.booking.dto.response.QrCodeResponse;
import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.entity.BookingDetail;
import com.autowash.features.booking.entity.Payment;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.booking.enums.PaymentMethod;
import com.autowash.features.booking.enums.PaymentStatus;
import com.autowash.features.booking.repository.BookingDetailRepository;
import com.autowash.features.booking.repository.BookingRepository;
import com.autowash.features.booking.repository.PaymentRepository;
import com.autowash.features.booking.mapper.BookingMapper;
import com.autowash.features.car.entity.Car;
import com.autowash.features.car.enums.CarStatus;
import com.autowash.features.car.repository.CarRepository;
import com.autowash.features.user.entity.CustomerProfile;
import com.autowash.features.user.entity.User;
import com.autowash.features.user.repository.UserRepository;
import com.autowash.features.washservice.entity.ServicePrice;
import com.autowash.features.washservice.repository.ServicePriceRepository;
import com.autowash.features.booking.entity.DailyOperationsConfig;
import com.autowash.features.booking.repository.DailyOperationsConfigRepository;
import com.autowash.features.booking.dto.response.AvailableSlotResponse;
import com.autowash.features.booking.dto.response.BusinessHoursResponse;

import com.autowash.features.promotion.entity.CustomerVoucher;
import com.autowash.features.promotion.entity.Promotion;
import com.autowash.features.promotion.enums.VoucherStatus;
import com.autowash.features.promotion.repository.CustomerVoucherRepository;

import com.autowash.features.wallet.entity.Wallet;
import com.autowash.features.wallet.entity.WalletTransaction;
import com.autowash.features.wallet.enums.WalletTransactionType;
import com.autowash.features.wallet.repository.WalletRepository;
import com.autowash.features.wallet.repository.WalletTransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final UserRepository userRepository;
    private final CarRepository carRepository;
    private final BookingMapper bookingMapper;
    private final DailyOperationsConfigRepository dailyOperationsConfigRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    private static final int MIN_BOOKING_BUFFER_MINUTES = 30;
    private static final int CANCEL_BUFFER_MINUTES = 60;

    @Transactional
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

        int subTotal = selectedPrices.stream()
                .mapToInt(ServicePrice::getPrice)
                .sum();

        int totalDuration = selectedPrices.stream()
                .mapToInt(ServicePrice::getDurationMinutes)
                .sum();

        validateSlotAvailable(request.getScheduledStartTime(), totalDuration);

        LocalDateTime expectedEndTime =
                request.getScheduledStartTime().plusMinutes(totalDuration);

        Booking booking = Booking.builder()
                .bookingCode(generateBookingCode())
                .user(customer)
                .vehicle(car)
                .scheduledStartTime(request.getScheduledStartTime())
                .expectedEndTime(expectedEndTime)
                .status(BookingStatus.PENDING)
                .customerNote(request.getCustomerNote())
                .totalPrice(subTotal)
                .qrContent(generateQrContent())
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
        savedBooking.setBookingDetails(details);

        // Tính giảm giá theo hạng thành viên (Tier)
        int tierDiscount = 0;
        CustomerProfile profile = customer.getCustomerProfile();
        if (profile != null && profile.getTierConfig() != null && profile.getTierConfig().getAutoDiscountPercent() != null) {
            double discountPercent = profile.getTierConfig().getAutoDiscountPercent().doubleValue();
            tierDiscount = (int) Math.round((subTotal * discountPercent) / 100);
        }

        // Tính giảm giá theo Voucher
        int voucherDiscount = 0;
        CustomerVoucher appliedVoucher = null;
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            String vCode = request.getVoucherCode().trim();
            appliedVoucher = customerVoucherRepository.findByUserIdAndVoucherCodeAndStatus(
                    customerId, vCode, VoucherStatus.AVAILABLE
            ).orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Voucher không hợp lệ hoặc đã được sử dụng"
            ));

            if (appliedVoucher.getExpiredAt() != null && appliedVoucher.getExpiredAt().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher đã hết hạn");
            }

            Promotion promo = appliedVoucher.getPromotion();
            if (promo != null) {
                if (promo.getDiscountAmount() != null) {
                    voucherDiscount = promo.getDiscountAmount();
                } else if (promo.getDiscountPercent() != null) {
                    double pct = promo.getDiscountPercent().doubleValue();
                    voucherDiscount = (int) Math.round((subTotal * pct) / 100);
                    if (promo.getMaxDiscountAmount() != null && voucherDiscount > promo.getMaxDiscountAmount()) {
                        voucherDiscount = promo.getMaxDiscountAmount();
                    }
                }
            }

            appliedVoucher.setStatus(VoucherStatus.USED);
            appliedVoucher.setUsedAt(LocalDateTime.now());
            customerVoucherRepository.save(appliedVoucher);
        }

        int finalPrice = Math.max(subTotal - tierDiscount - voucherDiscount, 0);

        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        LocalDateTime paidAt = null;

        if (PaymentMethod.WALLET.equals(request.getPaymentMethod())) {
            Wallet wallet = walletRepository.findByUserId(customerId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Tài khoản chưa được kích hoạt ví"
                    ));

            if (wallet.getBalance() < finalPrice) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Số dư ví không đủ để thanh toán"
                );
            }

            wallet.setBalance(wallet.getBalance() - finalPrice);
            walletRepository.save(wallet);

            paymentStatus = PaymentStatus.PAID;
            paidAt = LocalDateTime.now();

            WalletTransaction walletTx = WalletTransaction.builder()
                    .wallet(wallet)
                    .amount(finalPrice)
                    .transactionType(WalletTransactionType.PAYMENT)
                    .description("Thanh toán cho đơn đặt lịch: " + savedBooking.getBookingCode())
                    .build();
            walletTransactionRepository.save(walletTx);
        }

        Payment payment = Payment.builder()
                .booking(savedBooking)
                .appliedVoucher(appliedVoucher)
                .paymentMethod(request.getPaymentMethod())
                .subTotal(subTotal)
                .discountAmount(tierDiscount + voucherDiscount)
                .finalPrice(finalPrice)
                .paymentStatus(paymentStatus)
                .paidAt(paidAt)
                .build();

        paymentRepository.save(payment);
        savedBooking.setPayment(payment);

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

    @Transactional
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

    @Transactional
    public BookingResponse checkInByQr(String qrContent) {
        Booking booking = bookingRepository.findByQrContent(qrContent)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Mã QR không hợp lệ"
                ));

        if (Boolean.TRUE.equals(booking.getQrUsed())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã QR đã được sử dụng"
            );
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking không ở trạng thái có thể check-in"
            );
        }

        booking.setQrUsed(true);
        booking.setStatus(BookingStatus.ARRIVED);
        booking.setArrivedAt(LocalDateTime.now());

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional
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

    private void validateSlotAvailable(LocalDateTime scheduledStartTime, int totalDuration) {
        LocalDate date = scheduledStartTime.toLocalDate();
        DailyOperationsConfig config = dailyOperationsConfigRepository.findByConfigDateWithLock(date)
                .orElse(DailyOperationsConfig.builder()
                        .configDate(date)
                        .openTime(LocalTime.of(8, 0))
                        .closeTime(LocalTime.of(18, 0))
                        .bayCount(2)
                        .isActive(true)
                        .build());

        if (Boolean.FALSE.equals(config.getIsActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cửa hàng đóng cửa vào ngày này"
            );
        }

        LocalTime startTime = scheduledStartTime.toLocalTime();
        int transitBufferMinutes = 5;
        int neededDuration = totalDuration + transitBufferMinutes;
        LocalTime endTime = startTime.plusMinutes(neededDuration);

        if (startTime.isBefore(config.getOpenTime()) || endTime.isAfter(config.getCloseTime())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thời gian hẹn nằm ngoài khung giờ hoạt động của cửa hàng (" 
                    + config.getOpenTime() + " - " + config.getCloseTime() + ")"
            );
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<Booking> activeBookings = bookingRepository
                .findByScheduledStartTimeBetweenOrderByScheduledStartTimeAsc(startOfDay, endOfDay)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING 
                          || b.getStatus() == BookingStatus.ARRIVED 
                          || b.getStatus() == BookingStatus.IN_PROGRESS)
                .toList();

        LocalDateTime proposedEnd = scheduledStartTime.plusMinutes(neededDuration);
        boolean hasFreeBay = checkBayAvailability(scheduledStartTime, proposedEnd, activeBookings, config.getBayCount());

        if (!hasFreeBay) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không còn khoang rửa xe trống cho khung giờ đã chọn"
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
        return getAvailableSlots(date, 90);
    }

    public List<AvailableSlotResponse> getAvailableSlots(LocalDate date, int totalDurationMinutes) {
        if (date == null) {
            date = LocalDate.now();
        }

        DailyOperationsConfig config = dailyOperationsConfigRepository.findByConfigDate(date)
                .orElse(DailyOperationsConfig.builder()
                        .configDate(date)
                        .openTime(LocalTime.of(8, 0))
                        .closeTime(LocalTime.of(18, 0))
                        .bayCount(2)
                        .isActive(true)
                        .build());

        if (Boolean.FALSE.equals(config.getIsActive())) {
            return new ArrayList<>();
        }

        int transitBufferMinutes = 5;
        int neededDuration = totalDurationMinutes + transitBufferMinutes;

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<Booking> activeBookings = bookingRepository
                .findByScheduledStartTimeBetweenOrderByScheduledStartTimeAsc(startOfDay, endOfDay)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING 
                          || b.getStatus() == BookingStatus.ARRIVED 
                          || b.getStatus() == BookingStatus.IN_PROGRESS)
                .toList();

        List<AvailableSlotResponse> availableSlots = new ArrayList<>();
        LocalTime currentStart = config.getOpenTime();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minStartTimeAllowed = now.plusMinutes(MIN_BOOKING_BUFFER_MINUTES);

        while (currentStart.plusMinutes(neededDuration).isBefore(config.getCloseTime()) 
               || currentStart.plusMinutes(neededDuration).equals(config.getCloseTime())) {

            LocalDateTime proposedStart = date.atTime(currentStart);
            LocalDateTime proposedEnd = proposedStart.plusMinutes(neededDuration);

            boolean isTimeValid = proposedStart.isAfter(minStartTimeAllowed);

            if (isTimeValid) {
                boolean hasFreeBay = checkBayAvailability(proposedStart, proposedEnd, activeBookings, config.getBayCount());
                
                availableSlots.add(AvailableSlotResponse.builder()
                        .startTime(proposedStart)
                        .endTime(proposedEnd.minusMinutes(transitBufferMinutes))
                        .available(hasFreeBay)
                        .build());
            } else {
                availableSlots.add(AvailableSlotResponse.builder()
                        .startTime(proposedStart)
                        .endTime(proposedEnd.minusMinutes(transitBufferMinutes))
                        .available(false)
                        .build());
            }

            currentStart = currentStart.plusMinutes(30);
        }

        return availableSlots;
    }

    private boolean checkBayAvailability(LocalDateTime start, LocalDateTime end, List<Booking> bookings, int bayCount) {
        List<Booking> overlapping = bookings.stream()
                .filter(b -> b.getScheduledStartTime().isBefore(end) && b.getExpectedEndTime().isAfter(start))
                .toList();

        if (overlapping.isEmpty()) {
            return true;
        }

        if (overlapping.size() < bayCount) {
            return true;
        }

        List<LocalDateTime> checkpoints = new ArrayList<>();
        checkpoints.add(start);
        for (Booking b : overlapping) {
            if (b.getScheduledStartTime().isAfter(start) && b.getScheduledStartTime().isBefore(end)) {
                checkpoints.add(b.getScheduledStartTime());
            }
        }

        for (LocalDateTime point : checkpoints) {
            long concurrentCount = overlapping.stream()
                    .filter(b -> (b.getScheduledStartTime().isBefore(point) || b.getScheduledStartTime().isEqual(point))
                               && b.getExpectedEndTime().isAfter(point))
                    .count();
            
            if (concurrentCount >= bayCount) {
                return false;
            }
        }

        return true;
    }

    public BusinessHoursResponse getBusinessHoursForDate(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        DailyOperationsConfig config = dailyOperationsConfigRepository.findByConfigDate(date)
                .orElse(DailyOperationsConfig.builder()
                        .configDate(date)
                        .openTime(LocalTime.of(8, 0))
                        .closeTime(LocalTime.of(18, 0))
                        .bayCount(2)
                        .isActive(true)
                        .build());
        
        String startTimeStr = config.getOpenTime() != null ? config.getOpenTime().toString().substring(0, 5) : "08:00";
        String endTimeStr = config.getCloseTime() != null ? config.getCloseTime().toString().substring(0, 5) : "18:00";
        
        return BusinessHoursResponse.builder()
                .startTime(startTimeStr)
                .endTime(endTimeStr)
                .build();
    }
}


