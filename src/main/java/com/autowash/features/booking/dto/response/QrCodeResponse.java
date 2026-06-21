package com.autowash.features.booking.dto.response;

import com.autowash.features.booking.entity.Booking;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QrCodeResponse {

    private String bookingCode;

    // Có thể là text QR hoặc base64 image sau này
    private String qrContent;
}


