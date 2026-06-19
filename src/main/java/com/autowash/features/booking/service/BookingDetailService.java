package com.autowash.features.booking.service;

import com.autowash.features.booking.entity.Booking;



import com.autowash.features.booking.dto.BookingDetailResponse;
import com.autowash.features.booking.entity.BookingDetail;
import com.autowash.features.booking.repository.BookingDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class BookingDetailService {

    private final BookingDetailRepository bookingDetailRepository;

    // Lấy danh sách dịch vụ chi tiết của một booking
    public List<BookingDetailResponse> getDetailsByBookingId(Long bookingId) {
        return bookingDetailRepository.findByBookingId(bookingId)
                .stream()
                .map(this::toBookingDetailResponse)
                .toList();
    }

    // Lấy chi tiết một BookingDetail theo id
    public BookingDetailResponse getBookingDetailById(Long id) {
        BookingDetail detail = bookingDetailRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy chi tiết booking"
                ));

        return toBookingDetailResponse(detail);
    }

    // Convert Entity sang DTO Response
    private BookingDetailResponse toBookingDetailResponse(BookingDetail detail) {
        return BookingDetailResponse.builder()
                .id(detail.getId())
                .serviceId(detail.getServicePrice().getService().getId())
                .serviceName(detail.getServicePrice().getService().getName())
                .actualPrice(detail.getActualPrice())
                .actualDurationMinutes(detail.getActualDurationMinutes())
                .build();
    }
}


