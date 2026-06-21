package com.autowash.features.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendRegistrationOtp(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("AutoWash Pro - Email Verification OTP");
        message.setText(
                "Welcome to AutoWash Pro!\n\n"
                        + "Your OTP verification code is: " + otpCode + "\n\n"
                        + "This code will expire in 5 minutes.\n\n"
                        + "If you did not request this, please ignore this email."
        );

        mailSender.send(message);
    }
}

