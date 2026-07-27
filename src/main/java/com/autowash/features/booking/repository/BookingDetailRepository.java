package com.autowash.features.booking.repository;

import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.washservice.service.WashService;
import com.autowash.features.booking.entity.Booking;

import com.autowash.features.washservice.entity.Service;
import com.autowash.features.washservice.entity.ServicePrice;

import com.autowash.features.booking.entity.BookingDetail;
import com.autowash.features.analytics.dto.response.ServiceRatioResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {

    List<BookingDetail> findByBookingId(Long bookingId);

    void deleteByBookingId(Long bookingId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "SELECT setval(pg_get_serial_sequence('booking_details', 'id'), (SELECT COALESCE(MAX(id), 1) FROM booking_details))", nativeQuery = true)
    void fixSequenceId();

    @Query("""
        SELECT new com.autowash.features.analytics.dto.response.ServiceRatioResponse(
            s.name,
            s.name,
            COUNT(bd),
            COUNT(bd)
        )
        FROM BookingDetail bd
        JOIN bd.servicePrice sp
        JOIN sp.service s
        JOIN bd.booking b
        WHERE b.status <> com.autowash.features.booking.enums.BookingStatus.CANCELLED
        GROUP BY s.id, s.name
    """)
    List<ServiceRatioResponse> getServiceRatios();
}


