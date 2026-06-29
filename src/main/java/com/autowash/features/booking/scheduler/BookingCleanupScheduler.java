package com.autowash.features.booking.scheduler;

import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.booking.repository.BookingRepository;
import com.autowash.features.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingCleanupScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    // Chạy định kỳ mỗi 5 phút một lần để giải phóng các slot bị "bỏ quên"
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void autoCancelLateBookings() {
        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(15);
        
        // Tìm toàn bộ booking PENDING hoặc CONFIRM đã quá giờ hẹn 15 phút mà chưa check-in
        List<Booking> lateBookings = bookingRepository
                .findByStatusInAndScheduledStartTimeBefore(
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRM),
                        timeLimit
                );

        for (Booking booking : lateBookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            
            // Hoàn lại 80% tiền cho khách hàng
            bookingService.processRefund(booking, 0.8);
        }
    }
}
