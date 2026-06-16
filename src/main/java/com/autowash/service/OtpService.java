package com.autowash.service;

import com.autowash.entity.OtpToken;
import com.autowash.entity.User;
import com.autowash.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;

    @Transactional
    public OtpToken createOtp(User user, String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        String otpCode = generateOtpCode();

        OtpToken otpToken = OtpToken.builder()
                .user(user)
                .email(email)
                .otpCode(otpCode)
                .resendCount(0)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();

        return otpTokenRepository.save(otpToken);
    }

    @Transactional
    public OtpToken resendOtp(User user, String email) {
        OtpToken latestOtp = otpTokenRepository
                .findTopByUserAndEmailOrderByCreatedAtDesc(user, email)
                .orElse(null);

        int resendCount = 0;

        if (latestOtp != null) {
            resendCount = latestOtp.getResendCount() + 1;

            if (resendCount > 3) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "You have reached the maximum resend limit"
                );
            }
        }

        String otpCode = generateOtpCode();

        OtpToken newOtp = OtpToken.builder()
                .user(user)
                .email(email)
                .otpCode(otpCode)
                .resendCount(resendCount)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();

        return otpTokenRepository.save(newOtp);
    }

    @Transactional
    public boolean verifyOtp(String email, String otpCode) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        if (otpCode == null || otpCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OTP code is required"
            );
        }

        OtpToken otpToken = otpTokenRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "OTP not found"
                ));

        if (otpToken.getVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OTP has already been verified"
            );
        }

        if (otpToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OTP has expired"
            );
        }

        if (!otpToken.getOtpCode().equals(otpCode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid OTP code"
            );
        }

        otpToken.setVerified(true);
        otpTokenRepository.save(otpToken);

        return true;
    }

    private String generateOtpCode() {
        Random random = new Random();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }
}