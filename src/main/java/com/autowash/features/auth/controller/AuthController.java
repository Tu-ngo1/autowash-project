package com.autowash.features.auth.controller;

import com.autowash.features.washservice.entity.Service;

import com.autowash.features.auth.dto.request.LoginRequest;
import com.autowash.features.auth.dto.request.RegisterRequest;
import com.autowash.features.auth.dto.request.SendRegistrationOtpRequest;
import com.autowash.features.auth.dto.request.VerifyRegistrationOtpRequest;
import com.autowash.features.auth.dto.response.AuthResponse;
import com.autowash.features.auth.dto.response.MessageResponse;
import com.autowash.features.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/send-otp")
    public MessageResponse sendRegistrationOtp(@RequestBody SendRegistrationOtpRequest request) {
        authService.sendRegistrationOtp(request.getEmail());
        return new MessageResponse("M� OTP d� du?c g?i t?i email c?a b?n. Vui l�ng ki?m tra h?p thu.");
    }

    @PostMapping("/register/verify-otp")
    public MessageResponse verifyRegistrationOtp(@RequestBody VerifyRegistrationOtpRequest request) {
        authService.verifyRegistrationOtp(request.getEmail(), request.getOtp());
        return new MessageResponse("Email d� du?c x�c th?c.");
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public String logout() {
        return "Logout successful. Please remove token on client side.";
    }
}


