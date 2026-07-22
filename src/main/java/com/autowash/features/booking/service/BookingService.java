package com.autowash.features.booking.service;

import com.autowash.features.user.service.UserService;
import com.autowash.features.user.dto.request.UpdateUserPointsRequest;
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
import com.autowash.features.booking.dto.request.WalkInBookingRequest;
import com.autowash.features.car.entity.VehicleModel;
import com.autowash.features.car.repository.VehicleModelRepository;
import com.autowash.features.user.entity.TierConfig;
import com.autowash.features.user.enums.TierLevel;
import com.autowash.features.user.enums.UserStatus;
import com.autowash.features.user.enums.Role;
import com.autowash.features.user.repository.CustomerProfileRepository;
import com.autowash.features.user.repository.TierConfigRepository;
import com.autowash.features.washservice.entity.ServicePrice;
import com.autowash.features.washservice.repository.ServicePriceRepository;
import com.autowash.features.booking.entity.DailyOperationsConfig;
import com.autowash.features.booking.repository.DailyOperationsConfigRepository;
import com.autowash.features.booking.dto.response.AvailableSlotResponse;
import com.autowash.features.booking.dto.response.BusinessHoursResponse;
import com.autowash.features.booking.dto.response.WashBayResponse;
import com.autowash.features.booking.dto.response.AdminBookingListResponse;
import com.autowash.features.booking.enums.CancelRequestStatus;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;

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
    private final VehicleModelRepository vehicleModelRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final TierConfigRepository tierConfigRepository;

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

    @Transactional
    public BookingResponse createWalkInBooking(WalkInBookingRequest request) {
        User currentStaff = userService.getCurrentUserEntity();
        
        // 1. Xử lý tài khoản khách hàng
        User customer = null;
        if (request.getCustomerPhone() != null && !request.getCustomerPhone().trim().isEmpty()) {
            customer = userRepository.findByPhone(request.getCustomerPhone().trim()).orElse(null);
        }
        
        if (customer == null) {
            // Sử dụng tài khoản "Khách vãng lai mặc định" (walkin@autowash.com) hoặc tạo mới
            customer = userRepository.findByEmail("walkin@autowash.com").orElse(null);
            if (customer == null) {
                User newUser = User.builder()
                        .fullName("Khách vãng lai")
                        .email("walkin@autowash.com")
                        .phone("0000000000")
                        .username("walkin_customer")
                        .password("walkin_placeholder_password")
                        .role(Role.CUSTOMER)
                        .status(UserStatus.ACTIVE)
                        .build();
                User savedUser = userRepository.save(newUser);
                
                TierConfig memberTier = tierConfigRepository.findById(TierLevel.MEMBER).orElse(null);
                CustomerProfile profile = CustomerProfile.builder()
                        .user(savedUser)
                        .tierConfig(memberTier)
                        .rewardPoints(0)
                        .tierPoints(0)
                        .build();
                customerProfileRepository.save(profile);
                
                Wallet wallet = Wallet.builder()
                        .user(savedUser)
                        .balance(0)
                        .build();
                walletRepository.save(wallet);
                
                customer = userRepository.findById(savedUser.getId()).orElse(savedUser);
            }
        }
        
        // 2. Xử lý xe (Car)
        String licensePlate = request.getLicensePlate().trim();
        Car car = carRepository.findByLicensePlate(licensePlate).orElse(null);
        if (car == null) {
            VehicleModel vehicleModel = vehicleModelRepository.findById(request.getVehicleModelId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Không tìm thấy mẫu xe có ID: " + request.getVehicleModelId()
                    ));
            car = Car.builder()
                    .user(customer)
                    .licensePlate(licensePlate)
                    .vehicleModel(vehicleModel)
                    .status(CarStatus.ACTIVE)
                    .build();
            car = carRepository.save(car);
        }
        
        // 3. Tính toán chi phí
        List<ServicePrice> selectedPrices = request.getServiceIds().stream()
                .map(id -> servicePriceRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Không tìm thấy giá dịch vụ ID: " + id
                        )))
                .toList();

        int subTotal = selectedPrices.stream()
                .mapToInt(ServicePrice::getPrice)
                .sum();
                
        int totalDuration = selectedPrices.stream()
                .mapToInt(ServicePrice::getDurationMinutes)
                .sum();
        
        validateSlotAvailable(request.getScheduledStartTime(), totalDuration);
                
        // Áp dụng chiết khấu hạng thành viên (nếu có)
        int tierDiscount = 0;
        CustomerProfile profile = customer.getCustomerProfile();
        if (profile != null && profile.getTierConfig() != null && profile.getTierConfig().getAutoDiscountPercent() != null) {
            double discountPercent = profile.getTierConfig().getAutoDiscountPercent().doubleValue();
            tierDiscount = (int) Math.round((subTotal * discountPercent) / 100);
        }
        
        int finalPrice = Math.max(subTotal - tierDiscount, 0);
        
        // 4. Tạo Booking & Payment
        String bookingCode = generateBookingCode();
        String qrContent = qrCodeService.generateQrContent(bookingCode);
        LocalDateTime expectedEndTime = request.getScheduledStartTime().plusMinutes(totalDuration);
        
        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .user(customer)
                .vehicle(car)
                .scheduledStartTime(request.getScheduledStartTime())
                .expectedEndTime(expectedEndTime)
                .status(BookingStatus.ARRIVED) // Đặt trực tiếp thành ARRIVED vì tiếp nhận trực tiếp
                .customerNote(request.getCustomerNote())
                .totalPrice(subTotal)
                .qrContent(qrContent)
                .qrUsed(true) // Đã quét check-in trực tiếp
                .arrivedAt(LocalDateTime.now())
                .staff(currentStaff) // Gán nhân viên tạo lịch là nhân viên phụ trách chính
                .build();
                
        Booking savedBooking = bookingRepository.save(booking);
        
        // Tạo các BookingDetail
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
        
        Payment payment = Payment.builder()
                .booking(savedBooking)
                .paymentMethod(request.getPaymentMethod())
                .subTotal(subTotal)
                .discountAmount(tierDiscount)
                .finalPrice(finalPrice)
                .paymentStatus(PaymentStatus.PENDING) // Walk-in booking starts as PENDING payment
                .paidAt(null)
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
        String normalized = qrContent == null ? "" : qrContent.trim();

        if (!normalized.startsWith("AUTOWASH|BOOKING|")) {
            log.warn("QR validation failed. Invalid prefix. Received='{}' (len={})", 
                     normalized, normalized.length());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, 
                    "QR code không đúng định dạng hệ thống (yêu cầu prefix AUTOWASH|BOOKING|)"
            );
        }

        Booking booking = bookingRepository.findByQrContent(normalized)
                .orElseThrow(() -> {
                    log.warn("QR lookup failed. Received='{}' (len={})", 
                             normalized, normalized.length());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã QR không hợp lệ");
                });

        if (booking.getCancelRequestStatus() == CancelRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đặt lịch đang ở trạng thái chờ duyệt hủy");
        }

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

        if (nextStatus == BookingStatus.WASHED || nextStatus == BookingStatus.COMPLETED) {
            if (booking.getCompletedAt() == null) {
                booking.setCompletedAt(LocalDateTime.now());
            }
            Payment payment = booking.getPayment();
            if (payment != null && payment.getPaymentStatus() == PaymentStatus.PENDING) {
                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
            if (nextStatus == BookingStatus.COMPLETED && currentStatus != BookingStatus.COMPLETED) {
                awardPointsForCompletedBooking(booking);
            }
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
                        && (nextStatus == BookingStatus.WASHED || nextStatus == BookingStatus.COMPLETED))
                        || (currentStatus == BookingStatus.WASHED
                        && nextStatus == BookingStatus.COMPLETED);

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
        processRefund(booking, refundRate, null);
    }

    @Transactional
    public void processRefund(Booking booking, double refundRate, String customReason) {
        Payment payment = booking.getPayment();
        if (payment == null) {
            return;
        }

        // 1. Kiểm tra xác thực tức thời với PayOS nếu thanh toán PayOS đang ở trạng thái PENDING
        if (payment.getPaymentMethod() == PaymentMethod.PAYOS && payment.getPaymentStatus() == PaymentStatus.PENDING) {
            try {
                vn.payos.model.v2.paymentRequests.PaymentLink paymentLink = payOS.paymentRequests().get(booking.getId());
                if (vn.payos.model.v2.paymentRequests.PaymentLinkStatus.PAID.equals(paymentLink.getStatus())) {
                    payment.setPaymentStatus(PaymentStatus.PAID);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
            } catch (Exception e) {
                log.warn("Không thể kiểm tra PayOS status khi hoàn tiền: {}", e.getMessage());
            }
        }

        // 2. Chỉ hoàn tiền nếu giao dịch đã được thanh toán thành công (PAID)
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            Long userId = booking.getUser().getId();
            int originalPrice = payment.getFinalPrice();
            int refundAmount = (int) Math.round(originalPrice * refundRate);

            // Tự động khởi tạo ví người dùng nếu chưa có trong DB
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseGet(() -> walletRepository.save(
                            Wallet.builder()
                                    .user(booking.getUser())
                                    .balance(0)
                                    .build()
                    ));

            // Cộng tiền vào ví
            wallet.setBalance(wallet.getBalance() + refundAmount);
            walletRepository.save(wallet);

            // Ghi nhận giao dịch hoàn tiền vào ví
            String reasonText = customReason != null ? customReason : (refundRate == 1.0 ? "hủy lịch" : "đến trễ quá 15 phút");
            String note = String.format("Hoàn tiền %.0f%% lịch hẹn %s do %s", 
                    refundRate * 100, 
                    booking.getBookingCode(), 
                    reasonText);
                    
            WalletTransaction transaction = WalletTransaction.builder()
                    .wallet(wallet)
                    .amount(refundAmount)
                    .transactionType(WalletTransactionType.REFUND)
                    .description(note)
                    .build();
            walletTransactionRepository.save(transaction);

            // Cập nhật trạng thái hóa đơn
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        // 3. Hoàn trả Voucher lại trạng thái AVAILABLE nếu có sử dụng
        if (payment.getAppliedVoucher() != null) {
            CustomerVoucher voucher = payment.getAppliedVoucher();
            voucher.setStatus(VoucherStatus.AVAILABLE);
            voucher.setUsedAt(null);
            customerVoucherRepository.save(voucher);
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

    public List<BookingResponse> getPendingBookingsForToday() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        return bookingRepository.findByScheduledStartTimeBetweenOrderByScheduledStartTimeAsc(startOfDay, endOfDay)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.CONFIRM)
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse confirmPendingBooking(Long bookingId) {
        User currentStaff = userService.getCurrentUserEntity();
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lịch hẹn không ở trạng thái có thể check-in");
        }

        booking.setQrUsed(true);
        booking.setStatus(BookingStatus.ARRIVED);
        booking.setArrivedAt(LocalDateTime.now());
        booking.setStaff(currentStaff);

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    public List<BookingResponse> getQueueBookingsForToday() {
        return bookingRepository.findBookingsByStatus(BookingStatus.ARRIVED)
                .stream()
                .filter(b -> b.getBayNumber() == null) // Filter out assigned bookings
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<WashBayResponse> getWashingBaysStatus() {
        LocalDate today = LocalDate.now();
        DailyOperationsConfig config = dailyOperationsConfigRepository.findByConfigDate(today)
                .orElse(DailyOperationsConfig.builder()
                        .configDate(today)
                        .openTime(LocalTime.of(8, 0))
                        .closeTime(LocalTime.of(18, 0))
                        .bayCount(2)
                        .isActive(true)
                        .build());

        int bayCount = config.getBayCount() != null ? config.getBayCount() : 2;

        List<Booking> activeBookings = bookingRepository.findBookingsByStatus(BookingStatus.IN_PROGRESS);
        List<Booking> waitingInBays = bookingRepository.findBookingsByStatus(BookingStatus.ARRIVED)
                .stream()
                .filter(b -> b.getBayNumber() != null)
                .toList();

        List<Booking> allBayBookings = new ArrayList<>();
        allBayBookings.addAll(activeBookings);
        allBayBookings.addAll(waitingInBays);

        LocalDateTime limitTime = LocalDateTime.now().plusMinutes(5);
        List<Booking> queueBookings = new ArrayList<>(bookingRepository.findBookingsByStatus(BookingStatus.ARRIVED)
                .stream()
                .filter(b -> b.getBayNumber() == null)
                .filter(b -> !b.getScheduledStartTime().isAfter(limitTime)) // Only dispatch if within 5 mins of scheduled start time
                .sorted((b1, b2) -> {
                    int comp = b1.getScheduledStartTime().compareTo(b2.getScheduledStartTime());
                    if (comp != 0) {
                        return comp;
                    }
                    LocalDateTime a1 = b1.getArrivedAt() != null ? b1.getArrivedAt() : b1.getScheduledStartTime();
                    LocalDateTime a2 = b2.getArrivedAt() != null ? b2.getArrivedAt() : b2.getScheduledStartTime();
                    return a1.compareTo(a2);
                })
                .toList());

        int queueIndex = 0;
        List<WashBayResponse> bays = new ArrayList<>();
        for (int i = 1; i <= bayCount; i++) {
            final int bayNum = i;
            Booking bookingInBay = allBayBookings.stream()
                    .filter(b -> b.getBayNumber() != null && b.getBayNumber() == bayNum)
                    .findFirst()
                    .orElse(null);

            if (bookingInBay == null && queueIndex < queueBookings.size()) {
                Booking nextBooking = queueBookings.get(queueIndex++);
                nextBooking.setBayNumber(bayNum);
                bookingInBay = bookingRepository.save(nextBooking);
            }

            String bayStatus = "AVAILABLE";
            if (bookingInBay != null) {
                if (bookingInBay.getStatus() == BookingStatus.IN_PROGRESS) {
                    bayStatus = "BUSY";
                } else if (bookingInBay.getStatus() == BookingStatus.ARRIVED) {
                    bayStatus = "READY_TO_WASH";
                }
            }

            WashBayResponse bayResponse = WashBayResponse.builder()
                    .id(bayNum)
                    .name("Bay " + bayNum)
                    .type("Khoang rửa xe")
                    .status(bayStatus)
                    .booking(bookingInBay != null ? bookingMapper.toResponse(bookingInBay) : null)
                    .build();

            bays.add(bayResponse);
        }
        return bays;
    }

    @Transactional
    public BookingResponse assignBookingToBay(Long bookingId, Integer bayNumber) {
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getCancelRequestStatus() == CancelRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đặt lịch đang ở trạng thái chờ duyệt hủy");
        }

        if (booking.getStatus() != BookingStatus.ARRIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lịch hẹn phải ở trạng thái đã check-in (ARRIVED) mới có thể cho vào khoang.");
        }

        LocalDateTime limitTime = LocalDateTime.now().plusMinutes(5);
        if (booking.getScheduledStartTime().isAfter(limitTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa đến giờ hẹn để đưa xe vào khoang rửa (chỉ được đưa vào trước tối đa 5 phút).");
        }

        boolean bayOccupied = bookingRepository.findBookingsByStatus(BookingStatus.IN_PROGRESS)
                .stream()
                .anyMatch(b -> b.getBayNumber() != null && b.getBayNumber().equals(bayNumber));

        if (bayOccupied) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoang rửa " + bayNumber + " hiện đang có xe đang rửa.");
        }

        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setBayNumber(bayNumber);
        booking.setWashStartedAt(LocalDateTime.now());

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse completeWashingInBay(Integer bayNumber) {
        Booking booking = bookingRepository.findBookingsByStatus(BookingStatus.IN_PROGRESS)
                .stream()
                .filter(b -> b.getBayNumber() != null && b.getBayNumber().equals(bayNumber))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy xe đang rửa trong khoang " + bayNumber));

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());

        Payment payment = booking.getPayment();
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.PENDING) {
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse startWashingInBay(Integer bayNumber) {
        Booking booking = bookingRepository.findBookingsByStatus(BookingStatus.ARRIVED)
                .stream()
                .filter(b -> b.getBayNumber() != null && b.getBayNumber().equals(bayNumber))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "Không tìm thấy xe đang chờ rửa trong khoang " + bayNumber
                ));

        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setWashStartedAt(LocalDateTime.now());

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    public AdminBookingListResponse getAdminBookingsWithFilters(
            int page,
            int limit,
            BookingStatus status,
            CancelRequestStatus cancelRequestStatus,
            String search,
            String startDate,
            String endDate
    ) {
        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (cancelRequestStatus != null) {
                predicates.add(cb.equal(root.get("cancelRequestStatus"), cancelRequestStatus));
            }

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("user").get("fullName")), pattern),
                        cb.like(cb.lower(root.get("user").get("email")), pattern),
                        cb.like(cb.lower(root.get("user").get("phone")), pattern),
                        cb.like(cb.lower(root.get("vehicle").get("licensePlate")), pattern)
                );
                predicates.add(searchPredicate);
            }

            if (startDate != null && !startDate.trim().isEmpty()) {
                try {
                    LocalDate start = LocalDate.parse(startDate.trim());
                    predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledStartTime"), start.atStartOfDay()));
                } catch (Exception e) {
                    // Ignore invalid date format
                }
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                try {
                    LocalDate end = LocalDate.parse(endDate.trim());
                    predicates.add(cb.lessThanOrEqualTo(root.get("scheduledStartTime"), end.atTime(LocalTime.MAX)));
                } catch (Exception e) {
                    // Ignore invalid date format
                }
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "scheduledStartTime"));
        Page<Booking> bookingPage = bookingRepository.findAll(spec, pageRequest);

        List<BookingResponse> responses = bookingPage.getContent().stream()
                .map(bookingMapper::toResponse)
                .toList();

        return AdminBookingListResponse.builder()
                .bookings(responses)
                .total(bookingPage.getTotalElements())
                .build();
    }

    @Transactional
    public BookingResponse createCancelRequestByStaff(Long bookingId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lý do đề xuất hủy không được để trống");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt lịch"));

        if (booking.getStatus() != BookingStatus.PENDING && 
            booking.getStatus() != BookingStatus.CONFIRM && 
            booking.getStatus() != BookingStatus.ARRIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể gửi yêu cầu hủy cho đơn hàng đã rửa hoặc hoàn tất");
        }

        if (booking.getCancelRequestStatus() == CancelRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng này đã có yêu cầu hủy đang chờ duyệt");
        }

        User staffUser = userService.getCurrentUserEntity();

        booking.setCancelRequestStatus(CancelRequestStatus.PENDING);
        booking.setCancelRequestReason(reason.trim());
        booking.setCancelRequestedBy(staffUser);
        booking.setCancelRequestedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toResponse(saved);
    }

    @Transactional
    public BookingResponse approveCancelRequest(Long bookingId, String adminNote) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt lịch"));

        if (booking.getCancelRequestStatus() != CancelRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng này không ở trạng thái chờ duyệt hủy");
        }

        booking.setCancelRequestStatus(CancelRequestStatus.APPROVED);
        booking.setStatus(BookingStatus.CANCELLED);
        if (adminNote != null && !adminNote.trim().isEmpty()) {
            booking.setCancelRequestAdminNote(adminNote.trim());
        }

        if (booking.getPayment() != null) {
            Payment payment = booking.getPayment();
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        Booking saved = bookingRepository.save(booking);
        processRefund(saved, 1.0);

        return bookingMapper.toResponse(saved);
    }

    @Transactional
    public BookingResponse rejectCancelRequest(Long bookingId, String adminNote) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt lịch"));

        if (booking.getCancelRequestStatus() != CancelRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng này không ở trạng thái chờ duyệt hủy");
        }

        booking.setCancelRequestStatus(CancelRequestStatus.REJECTED);
        booking.setCancelRequestAdminNote(adminNote);

        // Bác bỏ yêu cầu hủy -> Đơn đặt lịch quay lại trạng thái xác nhận hoạt động bình thường, tuyệt đối không chuyển thành CANCELLED
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            booking.setStatus(BookingStatus.CONFIRM);
        }

        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toResponse(saved);
    }

    @Transactional
    public BookingResponse updateBookingStatusByAdmin(Long bookingId, BookingStatus status, String note) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt lịch"));

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chỉnh sửa trạng thái của đơn đặt lịch đã kết thúc (Hoàn thành hoặc Hủy)");
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(status);

        if (status == BookingStatus.ARRIVED) {
            booking.setQrUsed(true);
            if (booking.getArrivedAt() == null) {
                booking.setArrivedAt(LocalDateTime.now());
            }
        } else if (status == BookingStatus.IN_PROGRESS) {
            if (booking.getWashStartedAt() == null) {
                booking.setWashStartedAt(LocalDateTime.now());
            }
        } else if (status == BookingStatus.WASHED || status == BookingStatus.COMPLETED) {
            if (booking.getCompletedAt() == null) {
                booking.setCompletedAt(LocalDateTime.now());
            }
            if (booking.getPayment() != null && booking.getPayment().getPaymentStatus() == PaymentStatus.PENDING) {
                Payment payment = booking.getPayment();
                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
            if (status == BookingStatus.COMPLETED && oldStatus != BookingStatus.COMPLETED) {
                awardPointsForCompletedBooking(booking);
            }
        } else if (status == BookingStatus.CANCELLED && oldStatus != BookingStatus.CANCELLED) {
            User currentAdmin = userService.getCurrentUserEntity();
            booking.setCancelRequestedBy(currentAdmin);
            booking.setCancelRequestedAt(LocalDateTime.now());
            if (note != null && !note.trim().isEmpty()) {
                booking.setCancelRequestReason(note.trim());
                booking.setCancelRequestAdminNote(note.trim());
            }
            if (booking.getPayment() != null) {
                Payment payment = booking.getPayment();
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            }
            processRefund(booking, 1.0);
        }

        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toResponse(saved);
    }

    @Transactional
    public void deleteBookingByAdmin(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt lịch"));

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy đơn đặt lịch đã hoàn thành");
        }

        if (booking.getStatus() != BookingStatus.CANCELLED) {
            booking.setStatus(BookingStatus.CANCELLED);
            if (booking.getPayment() != null) {
                Payment payment = booking.getPayment();
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            }
            bookingRepository.save(booking);
            processRefund(booking, 1.0);
        }
    }

    private void awardPointsForCompletedBooking(Booking booking) {
        if (booking.getUser() != null && booking.getUser().getRole() == Role.CUSTOMER) {
            Payment payment = booking.getPayment();
            if (payment != null && payment.getFinalPrice() != null) {
                int earnedPoints = payment.getFinalPrice() / 1000;
                if (earnedPoints > 0) {
                    UpdateUserPointsRequest pointsRequest = new UpdateUserPointsRequest();
                    pointsRequest.setRankPointsDelta(earnedPoints);
                    pointsRequest.setRedeemPointsDelta(earnedPoints);
                    userService.updatePointsAndRecalculateTier(booking.getUser().getId(), pointsRequest);
                    log.info("Successfully awarded {} points to user {} for completing booking {}", 
                            earnedPoints, booking.getUser().getId(), booking.getBookingCode());
                }
            }
        }
    }
}


