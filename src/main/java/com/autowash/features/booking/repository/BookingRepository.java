package com.autowash.features.booking.repository;

import com.autowash.features.booking.entity.Payment;
import com.autowash.features.washservice.service.WashService;

import com.autowash.features.washservice.entity.Service;
import com.autowash.features.washservice.entity.ServicePrice;
import com.autowash.features.user.entity.User;

import com.autowash.features.analytics.dto.response.BookingStatusResponse;
import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        SELECT new com.autowash.features.analytics.dto.response.BookingStatusResponse(
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

    List<Booking> findByUserIdOrderByScheduledStartTimeDesc(Long userId);

    boolean existsByVehicleIdAndStatusIn(
            Long vehicleId,
            Collection<BookingStatus> statuses
    );

    boolean existsByScheduledStartTimeAndStatusIn(
            LocalDateTime scheduledStartTime,
            Collection<BookingStatus> statuses
    );

    List<Booking> findByScheduledStartTimeBetweenOrderByScheduledStartTimeAsc(
            LocalDateTime from,
            LocalDateTime to
    );

    List<Booking> findByStatusInAndScheduledStartTimeBefore(
            Collection<BookingStatus> statuses,
            LocalDateTime dateTime
    );

    Optional<Booking> findByQrContent(String qrContent);

    int countByUserId(Long userId);

    long countByStatus(BookingStatus status);

    long countByStatusNot(BookingStatus status);
}


