package com.autowash.features.auth.dto;

import lombok.Data;

@Data
public class VerifyOtpRequest {

    private String email;
    private String otpCode;
}
