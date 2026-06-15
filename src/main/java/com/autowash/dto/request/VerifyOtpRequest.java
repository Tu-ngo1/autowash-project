package com.autowash.dto.request;

import lombok.Data;

@Data
public class VerifyOtpRequest {

    private String email;
    private String otpCode;
}