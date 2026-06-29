package com.autowash.features.booking.service;

import com.autowash.features.user.service.UserService;
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
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    private final QrCodeService qrCodeService;
    private final PayOS payOS;
    private final UserService userService;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    @Value("${payos.test-mode:false}")
    private boolean payosTestMode;

    @Value("${payos.test-amount:10000}")
    private long payosTestAmount;

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
                .totalPrice(subTotal)
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
            savedBooking.setStatus(BookingStatus.CONFIRM);

            WalletTransaction walletTx = WalletTransaction.builder()
                    .wallet(wallet)
                    .amount(-finalPrice)
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

        BookingResponse response = bookingMapper.toResponse(savedBooking);

        if (PaymentMethod.PAYOS.equals(request.getPaymentMethod())) {
            try {
                long orderCode = savedBooking.getId();
                CreatePaymentLinkRequest payosRequest = CreatePaymentLinkRequest.builder()
                        .orderCode(orderCode)
                        .amount(payosTestMode ? payosTestAmount : (long) finalPrice)
                        .description("Booking " + savedBooking.getBookingCode())
                        .returnUrl(returnUrl)
                        .cancelUrl(cancelUrl)
                        .build();

                CreatePaymentLinkResponse payosResponse = payOS.paymentRequests().create(payosRequest);
                response.setCheckoutUrl(payosResponse.getCheckoutUrl());
            } catch (Exception e) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Không thể tạo link thanh toán PayOS: " + e.getMessage()
                );
            }
        }

        return response;
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
    public BookingResponse getBookingByIdAndUserId(Long bookingId, Long userId) {
        Booking booking = findBookingOrThrow(bookingId);
        if(!booking.getUser().getId().equals(userId)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem booking này");
        }
        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public void cancelBooking(Long customerId, Long bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (!booking.getUser().getId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền hủy booking này");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể hủy lịch ở trạng thái PENDING hoặc CONFIRM");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime latestCancelTime = booking.getScheduledStartTime().minusMinutes(CANCEL_BUFFER_MINUTES); // Hạn hủy 60 phút

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // TH 1: Khách hủy sớm trước giờ hẹn ít nhất 60 phút -> HOÀN TIỀN 100%
        if (now.isBefore(latestCancelTime) || now.isEqual(latestCancelTime)) {
            processRefund(booking, 1.0); // Hoàn tiền 100%
        } 
        // TH 2: Khách hủy trễ dưới 60 phút -> KHÔNG HOÀN TIỀN (Phạt 100% tiền cọc)
        else {
            Payment payment = booking.getPayment();
            if (payment != null && payment.getPaymentStatus() == PaymentStatus.PAID) {
                payment.setPaymentStatus(PaymentStatus.FAILED); // Đổi trạng thái thanh toán thành thất bại
                paymentRepository.save(payment);
            }
        }
    }

    @Transactional
    public BookingResponse checkInByQr(String qrContent) {
        User currentStaff = userService.getCurrentUserEntity();
        Booking booking = bookingRepository.findByQrContent(qrContent)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã QR không hợp lệ"));

        if (Boolean.TRUE.equals(booking.getQrUsed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã QR đã được sử dụng");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lịch hẹn không ở trạng thái có thể check-in");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lateDeadline = booking.getScheduledStartTime().plusMinutes(15); // Hạn đi trễ 15 phút

        // Khách đến trễ quá 15 phút -> Hủy lịch, hoàn tiền 80%
        if (now.isAfter(lateDeadline)) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setQrUsed(true);
            bookingRepository.save(booking);

            processRefund(booking, 0.8); // Hoàn tiền 80% (Phạt 20%)

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lịch hẹn đã bị hủy tự động do bạn đến trễ quá 15 phút. Hệ thống đã hoàn lại 80% số tiền vào ví của bạn."
            );
        }

        // Khách đến đúng giờ hoặc trễ dưới 15 phút -> Cho phép check-in vào khoang
        booking.setQrUsed(true);
        booking.setStatus(BookingStatus.ARRIVED);
        booking.setArrivedAt(now);
        booking.setStaff(currentStaff);
        if (now.isAfter(booking.getScheduledStartTime())) {
            booking.setLate(true); // Ghi nhận đi trễ dưới 15 phút để làm dữ liệu phân tích sau này
        }

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse updateBookingStatus(
            Long bookingId,
            UpdateBookingStatusRequest request
    ) {
        Booking booking = findBookingOrThrow(bookingId);
        User currentStaff = userService.getCurrentUserEntity();


        // KIỂM TRA BẢO MẬT:
        // Nếu lịch đã có nhân viên check-in, chỉ cho phép CHÍNH NHÂN VIÊN ĐÓ cập nhật trạng thái tiếp theo (IN_PROGRESS, WASHED...)
        if(booking.getStaff() != null && !booking.getStaff().getId().equals(currentStaff.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lịch hẹn này đã được tiếp nhận và check-in bởi nhân viên khác trong ca trực của họ.");
        }

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
                .qrImageBase64(qrCodeService.generateQrImageBase64(booking.getQrContent()))
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
                BookingStatus.CONFIRM,
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
                          || b.getStatus() == BookingStatus.CONFIRM
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
                (currentStatus == BookingStatus.PENDING && nextStatus == BookingStatus.CONFIRM)
                        || ((currentStatus == BookingStatus.PENDING || currentStatus == BookingStatus.CONFIRM)
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
                          || b.getStatus() == BookingStatus.CONFIRM
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

    @Transactional
    public void processRefund(Booking booking, double refundRate) {
        Payment payment = booking.getPayment();
        if (payment == null || payment.getPaymentStatus() != PaymentStatus.PAID) {
            return; // Chưa trả tiền thì không cần hoàn
        }

        if (payment.getPaymentMethod() == PaymentMethod.WALLET || payment.getPaymentMethod() == PaymentMethod.PAYOS) {
            Long userId = booking.getUser().getId();
            int originalPrice = payment.getFinalPrice();
            int refundAmount = (int) Math.round(originalPrice * refundRate);

            // 1. Tìm ví của user
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy ví người dùng"));

            // 2. Cộng lại số tiền hoàn
            wallet.setBalance(wallet.getBalance() + refundAmount);
            walletRepository.save(wallet);

            // 3. Ghi lịch sử giao dịch ví
            String note = String.format("Hoàn tiền %.0f%% lịch hẹn %s do %s", 
                    refundRate * 100, 
                    booking.getBookingCode(), 
                    refundRate == 1.0 ? "hủy lịch sớm" : "đến trễ quá 15 phút");
                    
            WalletTransaction transaction = WalletTransaction.builder()
                    .wallet(wallet)
                    .amount(refundAmount)
                    .transactionType(WalletTransactionType.REFUND)
                    .description(note)
                    .build();
            walletTransactionRepository.save(transaction);

            // 4. Đổi trạng thái hóa đơn
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }
    }

    @Transactional
    public BookingResponse verifyPayment(Long customerId, Long bookingId) {
        log.info("Verifying payment for booking ID: {}, customer ID: {}", bookingId, customerId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

        if (!booking.getUser().getId().equals(customerId)) {
            log.error("Permission denied. Booking belongs to user ID: {}, requested by customer ID: {}", 
                    booking.getUser().getId(), customerId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập lịch hẹn này");
        }

        if (BookingStatus.CONFIRM.equals(booking.getStatus())) {
            log.info("Booking ID {} is already CONFIRMED. Skipping verification.", bookingId);
            return bookingMapper.toResponse(booking);
        }

        if (booking.getPayment() != null && PaymentMethod.PAYOS.equals(booking.getPayment().getPaymentMethod())) {
            try {
                log.info("Fetching payment link info from PayOS for booking ID: {}", bookingId);
                vn.payos.model.v2.paymentRequests.PaymentLink paymentLink = 
                        payOS.paymentRequests().get(bookingId);
                
                String payosStatus = paymentLink.getStatus().toString();
                log.info("PayOS returned status: {} for booking ID: {}", payosStatus, bookingId);

                if (vn.payos.model.v2.paymentRequests.PaymentLinkStatus.PAID.equals(paymentLink.getStatus())) {
                    Payment payment = booking.getPayment();
                    payment.setPaymentStatus(PaymentStatus.PAID);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);

                    booking.setStatus(BookingStatus.CONFIRM);
                    bookingRepository.save(booking);
                    log.info("Successfully updated booking ID {} to CONFIRM and payment to PAID", bookingId);
                } else if (vn.payos.model.v2.paymentRequests.PaymentLinkStatus.CANCELLED.equals(paymentLink.getStatus()) ||
                           vn.payos.model.v2.paymentRequests.PaymentLinkStatus.EXPIRED.equals(paymentLink.getStatus()) ||
                           vn.payos.model.v2.paymentRequests.PaymentLinkStatus.FAILED.equals(paymentLink.getStatus())) {
                    Payment payment = booking.getPayment();
                    payment.setPaymentStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);

                    booking.setStatus(BookingStatus.CANCELLED);
                    bookingRepository.save(booking);
                    log.info("Successfully updated booking ID {} to CANCELLED and payment to FAILED", bookingId);
                } else {
                    log.warn("Payment link status is {} - not paid or cancelled yet.", payosStatus);
                }
            } catch (Exception e) {
                log.error("Error communicating with PayOS API for booking ID {}: {}", bookingId, e.getMessage(), e);
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Không thể xác thực trạng thái thanh toán với PayOS: " + e.getMessage()
                );
            }
        } else {
            log.warn("Booking ID {} payment method is not PAYOS or payment entity is null", bookingId);
        }

        return bookingMapper.toResponse(booking);
    }


}


