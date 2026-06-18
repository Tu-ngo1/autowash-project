package com.autowash.repository;

import com.autowash.entity.BookingDetail;
import com.autowash.dto.response.ServiceRatioResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {

    List<BookingDetail> findByBookingId(Long bookingId);

    @Query("""
        SELECT new com.autowash.dto.response.ServiceRatioResponse(
            s.name,
            s.name,
            COUNT(bd),
            COUNT(bd)
        )
        FROM BookingDetail bd
        JOIN bd.servicePrice sp
        JOIN sp.service s
        JOIN bd.booking b
        WHERE b.status <> com.autowash.enums.BookingStatus.CANCELLED
        GROUP BY s.id, s.name
    """)
    List<ServiceRatioResponse> getServiceRatios();
}
