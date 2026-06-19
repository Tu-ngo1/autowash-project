package com.autowash.features.booking.service;



import com.autowash.features.booking.dto.QrCodeResponse;
import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class QrCodeService {

    private final BookingRepository bookingRepository;

    // Tạo nội dung QR duy nhất cho booking
    public String generateQrContent(String bookingCode) {
        return "AUTOWASH|BOOKING|" + bookingCode + "|" + UUID.randomUUID();
    }

    // Customer lấy QR của booking
    public QrCodeResponse getQrCode(Long customerId, Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy booking"
                ));

        // Chỉ chủ booking mới được xem QR
        if (!booking.getUser().getId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền xem QR của booking này"
            );
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ booking ở trạng thái PENDING mới có QR hợp lệ"
            );
        }

        return QrCodeResponse.builder()
                .bookingCode(booking.getBookingCode())
                .qrContent(booking.getQrContent())
                .build();
    }

    // Staff quét QR để check-in
    public Booking checkInByQr(String qrContent) {

        Booking booking = bookingRepository.findByQrContent(qrContent)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Mã QR không hợp lệ"
                ));

        if (Boolean.TRUE.equals(booking.getQrUsed())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã QR này đã được sử dụng"
            );
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking không ở trạng thái có thể check-in"
            );
        }

        booking.setQrUsed(true);
        booking.setStatus(BookingStatus.ARRIVED);
        booking.setArrivedAt(LocalDateTime.now());

        return bookingRepository.save(booking);
    }
}


