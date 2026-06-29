package com.autowash.features.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInQrRequest {
    @NotBlank(message = "Mã QR không được để trống")
    private String qrContent;
}
