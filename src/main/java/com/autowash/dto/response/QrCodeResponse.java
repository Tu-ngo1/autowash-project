package com.autowash.dto.response;

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

