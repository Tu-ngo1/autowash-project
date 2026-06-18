package com.autowash.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QrCodeResponse {

    private String bookingCode;

    // Có thể là text QR hoặc base64 image sau này
    private String qrContent;
}

