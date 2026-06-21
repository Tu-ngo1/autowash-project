package com.autowash.features.auth.dto.request;

import lombok.Data;

@Data
public class VerifyRegistrationOtpRequest {

    private String email;
    private String otp;
}

