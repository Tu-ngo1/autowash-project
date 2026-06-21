package com.autowash.features.auth.dto.request;

import lombok.Data;

@Data
public class VerifyOtpRequest {

    private String email;
    private String otpCode;
}
