package com.autowash.features.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class EmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    public void sendRegistrationOtp(String toEmail, String otpCode) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            System.err.println("Resend API Key is not configured. Email not sent.");
            throw new IllegalStateException("Resend API Key is not configured");
        }

        String url = "https://api.resend.com/emails";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("from", fromEmail);
        body.put("to", List.of(toEmail));
        body.put("subject", "AutoWash Pro - Email Verification OTP");
        body.put("html", "<p>Welcome to AutoWash Pro!</p>"
                + "<p>Your OTP verification code is: <strong>" + otpCode + "</strong></p>"
                + "<p>This code will expire in 5 minutes.</p>"
                + "<p>If you did not request this, please ignore this email.</p>");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Email sent successfully via Resend API to " + toEmail);
            } else {
                System.err.println("Failed to send email via Resend API. Response: " + response.getBody());
                throw new RuntimeException("Resend API returned status code: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error calling Resend API: " + e.getMessage());
            throw new RuntimeException("Failed to send verification email via Resend", e);
        }
    }
}
