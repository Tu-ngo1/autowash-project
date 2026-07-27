package com.autowash.features.auth.service;



import com.autowash.features.auth.entity.OtpToken;
import com.autowash.features.user.entity.User;
import com.autowash.features.auth.enums.OtpPurpose;
import com.autowash.features.auth.repository.OtpTokenRepository;
import com.autowash.features.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final EmailService emailService;

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
        try {
            emailService.sendRegistrationOtp(user.getEmail(), otpCode);
        } catch (Exception e) {
            System.out.println("====== [TEST OTP] User: " + user.getEmail() + " | Code: " + otpCode + " ======");
        }
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

        try {
            emailService.sendRegistrationOtp(user.getEmail(), otpCode);
        } catch (Exception e) {
            System.out.println("====== [TEST OTP] Resend User: " + user.getEmail() + " | Code: " + otpCode + " ======");
        }
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

    @Transactional
    public void sendRegistrationOtp(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email already exists"
            );
        }

        String otpCode = generateOtpCode();

        OtpToken otpToken = OtpToken.builder()
                .email(normalizedEmail)
                .otpCode(otpCode)
                .resendCount(0)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .purpose(OtpPurpose.REGISTER)
                .build();

        otpTokenRepository.save(otpToken);
        try {
            emailService.sendRegistrationOtp(normalizedEmail, otpCode);
        } catch (Exception e) {
            System.out.println("====== [TEST OTP] Registration Email: " + normalizedEmail + " | Code: " + otpCode + " ======");
        }
    }

    @Transactional
    public void verifyRegistrationOtp(String email, String otpCode) {
        OtpToken otpToken = getLatestRegistrationOtp(email);

        assertOtpNotExpired(otpToken);
        assertOtpNotAlreadyVerified(otpToken);
        assertOtpCodeMatches(otpToken, otpCode);

        otpToken.setVerified(true);
        otpTokenRepository.save(otpToken);
    }

    @Transactional
    public void validateRegistrationOtpForSignup(String email, String otpCode) {
        OtpToken otpToken = getLatestRegistrationOtp(email);

        if (!Boolean.TRUE.equals(otpToken.getVerified())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please verify OTP before registering"
            );
        }

        assertOtpNotExpired(otpToken);
        assertOtpCodeMatches(otpToken, otpCode);
    }

    @Transactional
    public void consumeRegistrationOtp(String email) {
        otpTokenRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(normalizeEmail(email), OtpPurpose.REGISTER)
                .ifPresent(otpTokenRepository::delete);
    }

    @Transactional
    public void sendForgotPasswordOtp(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (!userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Email không tồn tại trong hệ thống"
            );
        }

        String otpCode = generateOtpCode();

        OtpToken otpToken = OtpToken.builder()
                .email(normalizedEmail)
                .otpCode(otpCode)
                .resendCount(0)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .purpose(OtpPurpose.FORGOT_PASSWORD)
                .build();

        otpTokenRepository.save(otpToken);
        try {
            emailService.sendForgotPasswordOtp(normalizedEmail, otpCode);
        } catch (Exception e) {
            System.out.println("====== [TEST OTP] Forgot Password Email: " + normalizedEmail + " | Code: " + otpCode + " ======");
        }
    }

    @Transactional
    public void verifyForgotPasswordOtp(String email, String otpCode) {
        OtpToken otpToken = getLatestForgotPasswordOtp(email);

        assertOtpNotExpired(otpToken);
        assertOtpNotAlreadyVerified(otpToken);
        assertOtpCodeMatches(otpToken, otpCode);

        otpToken.setVerified(true);
        otpTokenRepository.save(otpToken);
    }

    @Transactional
    public void validateForgotPasswordOtpForReset(String email, String otpCode) {
        OtpToken otpToken = getLatestForgotPasswordOtp(email);

        if (!Boolean.TRUE.equals(otpToken.getVerified())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng xác thực mã OTP trước khi đặt lại mật khẩu"
            );
        }

        assertOtpNotExpired(otpToken);
        assertOtpCodeMatches(otpToken, otpCode);
    }

    @Transactional
    public void consumeForgotPasswordOtp(String email) {
        otpTokenRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(normalizeEmail(email), OtpPurpose.FORGOT_PASSWORD)
                .ifPresent(otpTokenRepository::delete);
    }

    private OtpToken getLatestForgotPasswordOtp(String email) {
        return otpTokenRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(normalizeEmail(email), OtpPurpose.FORGOT_PASSWORD)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Mã OTP không tồn tại hoặc đã hết hạn"
                ));
    }

    private OtpToken getLatestRegistrationOtp(String email) {
        return otpTokenRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(normalizeEmail(email), OtpPurpose.REGISTER)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "OTP not found"
                ));
    }

    private void assertOtpNotExpired(OtpToken otpToken) {
        if (otpToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OTP has expired"
            );
        }
    }

    private void assertOtpNotAlreadyVerified(OtpToken otpToken) {
        if (Boolean.TRUE.equals(otpToken.getVerified())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OTP has already been verified"
            );
        }
    }

    private void assertOtpCodeMatches(OtpToken otpToken, String otpCode) {
        if (otpCode == null || otpCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OTP code is required"
            );
        }

        if (!otpToken.getOtpCode().equals(otpCode.trim())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid OTP code"
            );
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        return email.trim().toLowerCase();
    }

    private String generateOtpCode() {
        Random random = new Random();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }
}

