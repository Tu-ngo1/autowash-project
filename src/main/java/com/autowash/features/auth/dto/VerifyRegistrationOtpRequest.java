package com.autowash.features.auth.dto;

import lombok.Data;

@Data
public class VerifyRegistrationOtpRequest {

    private String email;
    private String otp;
}

