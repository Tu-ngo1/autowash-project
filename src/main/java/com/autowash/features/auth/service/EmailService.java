package com.autowash.features.auth.service;



import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    public void sendRegistrationOtp(String email, String otpCode) {
        log.info("Registration OTP sent to {}: {}", email, otpCode);
    }
}

