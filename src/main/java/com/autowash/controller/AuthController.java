package com.autowash.controller;

import com.autowash.dto.request.LoginRequest;
import com.autowash.dto.request.RegisterRequest;
import com.autowash.dto.request.SendRegistrationOtpRequest;
import com.autowash.dto.request.VerifyRegistrationOtpRequest;
import com.autowash.dto.response.AuthResponse;
import com.autowash.dto.response.MessageResponse;
import com.autowash.service.AuthService;
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
        return new MessageResponse("Mã OTP đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư.");
    }

    @PostMapping("/register/verify-otp")
    public MessageResponse verifyRegistrationOtp(@RequestBody VerifyRegistrationOtpRequest request) {
        authService.verifyRegistrationOtp(request.getEmail(), request.getOtp());
        return new MessageResponse("Email đã được xác thực.");
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
