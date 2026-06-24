package com.autowash.features.booking.dto.response;

import com.autowash.features.booking.entity.Booking;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QrCodeResponse {
    private String bookingCode;
    private String qrContent;

    // FE dùng chuỗi này để hiện ảnh QR
    private String qrImageBase64;
}


