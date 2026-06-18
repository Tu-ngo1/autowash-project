package com.autowash.repository;

import com.autowash.dto.response.BookingResponse;
import com.autowash.dto.response.BookingStatusResponse;
import com.autowash.entity.Booking;
import com.autowash.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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


    int countByUserId(Long userId);

}
