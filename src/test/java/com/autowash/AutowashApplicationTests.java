package com.autowash;

import com.autowash.features.auth.dto.request.LoginRequest;
import com.autowash.features.auth.dto.response.AuthResponse;
import com.autowash.features.auth.service.AuthService;
import com.autowash.features.booking.dto.request.CreateBookingRequest;
import com.autowash.features.booking.dto.response.BookingResponse;
import com.autowash.features.booking.dto.response.AvailableSlotResponse;
import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.repository.BookingRepository;
import com.autowash.features.booking.repository.PaymentRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.autowash.features.booking.service.BookingService;
import com.autowash.features.booking.enums.PaymentMethod;
import com.autowash.features.user.entity.User;
import com.autowash.features.user.enums.Role;
import com.autowash.features.user.enums.UserStatus;
import com.autowash.features.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.autowash.features.booking.dto.request.ReviewRequest;
import com.autowash.features.booking.dto.response.ReviewResponse;
import com.autowash.features.booking.entity.Review;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.booking.repository.ReviewRepository;
import com.autowash.features.booking.service.ReviewService;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.autowash.features.user.service.UserService;
import com.autowash.features.user.repository.CustomerProfileRepository;
import com.autowash.features.promotion.repository.PromotionRepository;
import com.autowash.features.promotion.repository.CustomerVoucherRepository;
import com.autowash.features.user.repository.TierConfigRepository;
import com.autowash.features.promotion.entity.Promotion;
import com.autowash.features.promotion.entity.CustomerVoucher;
import com.autowash.features.user.entity.CustomerProfile;
import com.autowash.features.user.entity.TierConfig;
import com.autowash.features.user.controller.CustomerController.CustomerVoucherResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AutowashApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private CustomerVoucherRepository customerVoucherRepository;

    @Autowired
    private TierConfigRepository tierConfigRepository;

    private static final String TEST_USERNAME = "testloginuser";
    private static final String TEST_PHONE = "0999999999";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    @AfterEach
    void cleanUp() {
        userRepository.findByPhone(TEST_PHONE).ifPresent(user -> userRepository.delete(user));
        cleanBookingsForVehicles(List.of(4L, 5L));
    }

    private void cleanBookingsForVehicles(List<Long> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) return;
        for (Long vehicleId : vehicleIds) {
            List<Long> bookingIds = jdbcTemplate.queryForList(
                    "SELECT id FROM BOOKINGS WHERE vehicle_id = ?",
                    Long.class,
                    vehicleId
            );
            if (!bookingIds.isEmpty()) {
                for (Long bId : bookingIds) {
                    jdbcTemplate.update("DELETE FROM reviews WHERE booking_id = ?", bId);
                    jdbcTemplate.update("DELETE FROM BOOKING_DETAILS WHERE booking_id = ?", bId);
                    jdbcTemplate.update("DELETE FROM PAYMENTS WHERE booking_id = ?", bId);
                    jdbcTemplate.update("DELETE FROM BOOKINGS WHERE id = ?", bId);
                }
            }
        }
    }

    @Test
    void testLoginWithEmailAndPhone() {
        // 1. Create and save test user
        User user = User.builder()
                .fullName("Test Login User")
                .username(TEST_USERNAME)
                .email("testloginuser@example.com")
                .phone(TEST_PHONE)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);

        // 2. Test Login using Email
        LoginRequest emailLoginRequest = new LoginRequest();
        emailLoginRequest.setUsernameOrPhone("testloginuser@example.com");
        emailLoginRequest.setPassword(TEST_PASSWORD);

        AuthResponse emailLoginResponse = authService.login(emailLoginRequest);
        assertNotNull(emailLoginResponse);
        assertNotNull(emailLoginResponse.getToken());
        assertEquals("CUSTOMER", emailLoginResponse.getRole());

        // 3. Test Login using Phone
        LoginRequest phoneLoginRequest = new LoginRequest();
        phoneLoginRequest.setUsernameOrPhone(TEST_PHONE);
        phoneLoginRequest.setPassword(TEST_PASSWORD);

        AuthResponse phoneLoginResponse = authService.login(phoneLoginRequest);
        assertNotNull(phoneLoginResponse);
        assertNotNull(phoneLoginResponse.getToken());
        assertEquals("CUSTOMER", phoneLoginResponse.getRole());
    }

    @Test
    void testDoubleBooking() {
        LocalDateTime bookingTime = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0);
        LocalDate bookingDate = bookingTime.toLocalDate();

        // Customer 4 booking using vehicle 5 (free)
        CreateBookingRequest req1 = new CreateBookingRequest();
        req1.setVehicleId(5L); // car 5 (SMALL)
        req1.setScheduledStartTime(bookingTime);
        req1.setServiceIds(List.of(1L)); // service 1
        req1.setPaymentMethod(PaymentMethod.CASH);

        // Customer 7 booking using vehicle 4 (free) at the same time
        CreateBookingRequest req2 = new CreateBookingRequest();
        req2.setVehicleId(4L); // car 4 (SMALL)
        req2.setScheduledStartTime(bookingTime);
        req2.setServiceIds(List.of(1L)); // service 1
        req2.setPaymentMethod(PaymentMethod.CASH);

        BookingResponse res1 = null;
        BookingResponse res2 = null;
        try {
            res1 = bookingService.createBooking(4L, req1);
            assertNotNull(res1);

            res2 = bookingService.createBooking(7L, req2);
            assertNotNull(res2);

            // Verify that the 11:00 slot is now UNAVAILABLE (available = false)
            List<AvailableSlotResponse> slots = bookingService.getAvailableSlots(bookingDate, 40);
            assertFalse(slots.isEmpty());

            AvailableSlotResponse slot11 = slots.stream()
                    .filter(s -> s.getStartTime().toLocalTime().equals(LocalTime.of(11, 0)))
                    .findFirst()
                    .orElse(null);

            assertNotNull(slot11);
            assertFalse(slot11.getAvailable(), "Slot 11:00 should be unavailable as all bays are occupied");
        } finally {
            cleanBookingsForVehicles(List.of(4L, 5L));
        }
    }

    @Test
    void testPaymentLinkFields() {
        for (java.lang.reflect.Method method : vn.payos.model.v2.paymentRequests.PaymentLink.class.getDeclaredMethods()) {
            System.out.println("METHOD: " + method.getReturnType().getName() + " " + method.getName());
        }
    }

    @Test
    void testCreateReviewSuccessAndValidation() {
        LocalDateTime bookingTime = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0);

        // 1. Create a booking for user 4L
        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehicleId(5L); // car 5 (SMALL)
        req.setScheduledStartTime(bookingTime);
        req.setServiceIds(List.of(1L)); // service 1
        req.setPaymentMethod(PaymentMethod.CASH);

        BookingResponse res = null;
        try {
            res = bookingService.createBooking(4L, req);
            assertNotNull(res);
            Long bookingId = res.getId();

            // Find the booking entity
            Booking booking = bookingRepository.findById(bookingId).orElseThrow();

            // Test Case 1: Try to review when status is still PENDING/CONFIRM
            ReviewRequest reviewReq = new ReviewRequest();
            reviewReq.setBookingId(bookingId);
            reviewReq.setRating(5);
            reviewReq.setComment("Dịch vụ rất tốt!");

            ResponseStatusException exNotCompleted = assertThrows(ResponseStatusException.class, () -> {
                reviewService.createReview(4L, reviewReq);
            });
            assertEquals(HttpStatus.BAD_REQUEST, exNotCompleted.getStatusCode());
            assertTrue(exNotCompleted.getReason().contains("Chỉ có thể đánh giá lịch hẹn đã hoàn thành"));

            // Test Case 2: Update status to COMPLETED and test review from a different user (7L) - should fail with FORBIDDEN
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);

            ResponseStatusException exForbidden = assertThrows(ResponseStatusException.class, () -> {
                reviewService.createReview(7L, reviewReq);
            });
            assertEquals(HttpStatus.FORBIDDEN, exForbidden.getStatusCode());
            assertTrue(exForbidden.getReason().contains("Bạn không có quyền đánh giá đơn đặt lịch này"));

            // Test Case 3: Create review successfully with correct user (4L)
            ReviewResponse reviewRes = reviewService.createReview(4L, reviewReq);
            assertNotNull(reviewRes);
            assertEquals(5, reviewRes.getRating());
            assertEquals("Dịch vụ rất tốt!", reviewRes.getComment());
            assertEquals(bookingId, reviewRes.getBookingId());

            // Test Case 4: Try to review the same booking again - should fail with BAD_REQUEST
            ResponseStatusException exDuplicate = assertThrows(ResponseStatusException.class, () -> {
                reviewService.createReview(4L, reviewReq);
            });
            assertEquals(HttpStatus.BAD_REQUEST, exDuplicate.getStatusCode());
            assertTrue(exDuplicate.getReason().contains("Lịch hẹn này đã được đánh giá trước đó"));

            // Test Case 5: Get my reviews list
            List<ReviewResponse> myReviews = reviewService.getMyReviews(4L);
            assertFalse(myReviews.isEmpty());
            boolean hasReview = myReviews.stream().anyMatch(r -> r.getBookingId().equals(bookingId));
            assertTrue(hasReview);

        } finally {
            cleanBookingsForVehicles(List.of(5L));
        }
    }

    @Test
    void testLoyaltyRedeemSuccessAndValidation() {
        // 1. Lấy thông tin CustomerProfile của user 4L
        User user = userRepository.findById(4L).orElseThrow();
        CustomerProfile profile = customerProfileRepository.findByUser(user).orElseThrow();
        int originalPoints = profile.getRewardPoints() != null ? profile.getRewardPoints() : 0;

        // 2. Tạo một Promotion dùng để test
        TierConfig memberTier = tierConfigRepository.findAll().stream()
                .filter(t -> t.getTierLevel() == com.autowash.features.user.enums.TierLevel.MEMBER)
                .findFirst()
                .orElseThrow();

        Promotion testPromo = Promotion.builder()
                .voucherCode("TESTREDEEM")
                .campaignName("Test Campaign")
                .pointCost(100)
                .targetTier(memberTier)
                .discountAmount(50000)
                .active(true)
                .startAt(LocalDateTime.now().minusDays(1))
                .endAt(LocalDateTime.now().plusDays(10))
                .build();
        testPromo = promotionRepository.save(testPromo);
        Long promoId = testPromo.getId();

        List<Long> voucherIdsToDelete = new java.util.ArrayList<>();

        try {
            // Test Case 1: Thử đổi khi không đủ điểm
            profile.setRewardPoints(50);
            customerProfileRepository.save(profile);

            ResponseStatusException exPoints = assertThrows(ResponseStatusException.class, () -> {
                userService.redeemVoucher(4L, promoId);
            });
            assertEquals(HttpStatus.BAD_REQUEST, exPoints.getStatusCode());
            assertTrue(exPoints.getReason().contains("Số điểm thưởng tích lũy không đủ"));

            // Test Case 2: Đổi thành công khi đủ điểm
            profile.setRewardPoints(150);
            customerProfileRepository.save(profile);

            CustomerVoucherResponse response = userService.redeemVoucher(4L, promoId);
            assertNotNull(response);
            voucherIdsToDelete.add(response.getId());
            assertEquals(promoId, response.getPromotionId());
            assertTrue(response.getVoucherCode().startsWith("TESTREDEEM-"));
            assertEquals("Test Campaign", response.getCampaignName());
            assertEquals(50000, response.getDiscountAmount());
            assertEquals("AVAILABLE", response.getStatus());

            // Kiểm tra điểm đã bị trừ: ban đầu 150 - cost 100 = 50
            CustomerProfile updatedProfile = customerProfileRepository.findByUser(user).orElseThrow();
            assertEquals(50, updatedProfile.getRewardPoints());

            // Test Case 3: Thử đổi khi Promotion đã ngừng hoạt động (active = false)
            testPromo.setActive(false);
            promotionRepository.save(testPromo);

            ResponseStatusException exActive = assertThrows(ResponseStatusException.class, () -> {
                userService.redeemVoucher(4L, promoId);
            });
            assertEquals(HttpStatus.BAD_REQUEST, exActive.getStatusCode());
            assertTrue(exActive.getReason().contains("Chương trình khuyến mại đã ngừng hoạt động"));

        } finally {
            // Dọn dẹp dữ liệu test
            for (Long cvId : voucherIdsToDelete) {
                customerVoucherRepository.deleteById(cvId);
            }
            promotionRepository.deleteById(promoId);

            // Phục hồi lại điểm gốc của user
            profile.setRewardPoints(originalPoints);
            customerProfileRepository.save(profile);
        }
    }
}


