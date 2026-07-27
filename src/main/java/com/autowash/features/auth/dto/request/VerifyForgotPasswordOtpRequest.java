package com.autowash.features.auth.dto.request;

import lombok.Data;

@Data
public class VerifyForgotPasswordOtpRequest {

    private String email;
    private String otp;
}
