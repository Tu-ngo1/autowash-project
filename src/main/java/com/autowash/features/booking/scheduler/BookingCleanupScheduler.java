package com.autowash.features.booking.scheduler;

import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.booking.repository.BookingRepository;
import com.autowash.features.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    // Chạy định kỳ mỗi 5 phút một lần (chu kỳ: phút 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void autoCancelLateBookings() {
        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(15);
        
        List<Booking> lateBookings = bookingRepository
                .findByStatusInAndScheduledStartTimeBefore(
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRM),
                        timeLimit
                );

        if (!lateBookings.isEmpty()) {
            log.info("Auto-canceling {} late bookings exceeded 15 minutes", lateBookings.size());
        }

        for (Booking booking : lateBookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            
            // Hoàn lại 80% tiền vào Ví cho khách hàng
            bookingService.processRefund(booking, 0.8, "tự động hủy do trễ quá 15 phút");
            log.info("Successfully auto-canceled booking {} and refunded 80% to customer wallet", booking.getBookingCode());
        }
    }
}
