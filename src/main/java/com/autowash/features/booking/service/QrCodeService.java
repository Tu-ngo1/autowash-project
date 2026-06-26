package com.autowash.features.booking.service;

import com.autowash.features.booking.dto.response.QrCodeResponse;
import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.booking.repository.BookingRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;
import java.time.LocalDateTime;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class QrCodeService {

    private final BookingRepository bookingRepository;

    public String generateQrContent(String bookingCode) {
        return "AUTOWASH|BOOKING|" + bookingCode + "|" + UUID.randomUUID();
    }

    public String generateQrImageBase64(String qrContent) {
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(qrContent, BarcodeFormat.QR_CODE, 250, 250);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);

            return "data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể tạo QR code"
            );
        }
    }

    public QrCodeResponse getQrCode(Long customerId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy booking"
                ));

        if (!booking.getUser().getId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền xem QR này"
            );
        }

        return QrCodeResponse.builder()
                .bookingCode(booking.getBookingCode())
                .qrContent(booking.getQrContent())
                .qrImageBase64(generateQrImageBase64(booking.getQrContent()))
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
