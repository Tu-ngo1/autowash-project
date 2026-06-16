package com.autowash.repository;

import com.autowash.dto.response.BookingStatusResponse;
import com.autowash.entity.Booking;
import com.autowash.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        SELECT new com.autowash.dto.response.BookingStatusResponse(
            b.status,
            COUNT(b)
        )
        FROM Booking b
        GROUP BY b.status
    """)
    List<BookingStatusResponse> countBookingsByStatus();

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.vehicle
        LEFT JOIN FETCH b.bookingDetails bd
        LEFT JOIN FETCH bd.servicePrice sp
        LEFT JOIN FETCH sp.service
        LEFT JOIN FETCH b.payment
        WHERE :status IS NULL OR b.status = :status
        ORDER BY b.scheduledStartTime DESC
    """)
    List<Booking> findBookingsByStatus(@Param("status") BookingStatus status);
    Optional<Booking> findByBookingCode(String bookingCode);

    // Customer xem booking của mình
    List<Booking> findByUserIdOrderByScheduledStartTimeDesc(Long userId);

    // Kiểm tra xe có booking chưa hoàn thành không
    boolean existsByVehicleIdAndStatusIn(
            Long vehicleId,
            Collection<BookingStatus> statuses
    );

    // Validate trùng slot
    boolean existsByScheduledStartTimeAndStatusIn(
            LocalDateTime scheduledStartTime,
            Collection<BookingStatus> statuses
    );

    // Staff xem hàng đợi hôm nay
    List<Booking> findByScheduledStartTimeBetweenOrderByScheduledStartTimeAsc(
            LocalDateTime from,
            LocalDateTime to
    );
    Optional<Booking> findByQrContent(String qrContent);

    int countByUserId(Long userId);
}
