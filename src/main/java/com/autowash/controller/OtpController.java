package com.autowash.controller;

import com.autowash.dto.request.CreateOtpRequest;
import com.autowash.dto.request.VerifyOtpRequest;
import com.autowash.entity.OtpToken;
import com.autowash.entity.User;
import com.autowash.service.OtpService;
import com.autowash.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;
    private final UserService userService;

    @PostMapping("/create")
    public String createOtp(@RequestBody CreateOtpRequest request) {
        User user = userService.getCurrentUserEntity();

        OtpToken otpToken = otpService.createOtp(user, request.getEmail());

        return "OTP created successfully. OTP code: " + otpToken.getOtpCode();
    }

    @PostMapping("/resend")
    public String resendOtp(@RequestBody CreateOtpRequest request) {
        User user = userService.getCurrentUserEntity();

        OtpToken otpToken = otpService.resendOtp(user, request.getEmail());

        return "OTP resent successfully. OTP code: " + otpToken.getOtpCode();
    }

    @PostMapping("/verify")
    public String verifyOtp(@RequestBody VerifyOtpRequest request) {
        otpService.verifyOtp(request.getEmail(), request.getOtpCode());

        return "OTP verified successfully";
    }
}