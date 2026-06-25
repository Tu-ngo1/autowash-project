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
    void testLoginWithUsernameAndPhone() {
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

        // 2. Test Login using Username
        LoginRequest usernameLoginRequest = new LoginRequest();
        usernameLoginRequest.setUsernameOrPhone(TEST_USERNAME);
        usernameLoginRequest.setPassword(TEST_PASSWORD);

        AuthResponse usernameLoginResponse = authService.login(usernameLoginRequest);
        assertNotNull(usernameLoginResponse);
        assertNotNull(usernameLoginResponse.getToken());
        assertEquals("CUSTOMER", usernameLoginResponse.getRole());

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
}

