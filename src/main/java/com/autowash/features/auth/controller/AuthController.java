package com.autowash.features.auth.controller;

import com.autowash.features.washservice.entity.Service;

import com.autowash.features.auth.dto.request.LoginRequest;
import com.autowash.features.auth.dto.request.RegisterRequest;
import com.autowash.features.auth.dto.request.SendForgotPasswordOtpRequest;
import com.autowash.features.auth.dto.request.SendRegistrationOtpRequest;
import com.autowash.features.auth.dto.request.VerifyForgotPasswordOtpRequest;
import com.autowash.features.auth.dto.request.VerifyRegistrationOtpRequest;
import com.autowash.features.auth.dto.request.ResetPasswordRequest;
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
        return new MessageResponse("Mã OTP đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư.");
    }

    @PostMapping("/register/verify-otp")
    public MessageResponse verifyRegistrationOtp(@RequestBody VerifyRegistrationOtpRequest request) {
        authService.verifyRegistrationOtp(request.getEmail(), request.getOtp());
        return new MessageResponse("Email đã được xác thực.");
    }

    @PostMapping("/forgot-password/send-otp")
    public MessageResponse sendForgotPasswordOtp(@RequestBody SendForgotPasswordOtpRequest request) {
        authService.sendForgotPasswordOtp(request.getEmail());
        return new MessageResponse("Mã OTP khôi phục mật khẩu đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư.");
    }

    @PostMapping("/forgot-password/verify-otp")
    public MessageResponse verifyForgotPasswordOtp(@RequestBody VerifyForgotPasswordOtpRequest request) {
        authService.verifyForgotPasswordOtp(request.getEmail(), request.getOtp());
        return new MessageResponse("Mã OTP đã được xác nhận thành công.");
    }

    @PostMapping("/forgot-password/reset")
    public MessageResponse resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new MessageResponse("Đặt lại mật khẩu thành công. Vui lòng đăng nhập với mật khẩu mới.");
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


