package com.autowash.dto.request;

import lombok.Data;

@Data
public class VerifyRegistrationOtpRequest {

    private String email;
    private String otp;
}
