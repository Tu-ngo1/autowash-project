package com.autowash.features.auth.service;



import com.autowash.features.auth.dto.request.LoginRequest;
import com.autowash.features.auth.dto.request.RegisterRequest;
import com.autowash.features.auth.dto.request.ResetPasswordRequest;
import com.autowash.features.auth.dto.response.AuthResponse;
import com.autowash.features.user.entity.CustomerProfile;
import com.autowash.features.user.entity.TierConfig;
import com.autowash.features.user.entity.User;
import com.autowash.features.user.enums.Role;
import com.autowash.features.user.enums.TierLevel;
import com.autowash.features.user.enums.UserStatus;
import com.autowash.infrastructure.security.JwtService;
import com.autowash.features.user.repository.CustomerProfileRepository;
import com.autowash.features.user.repository.TierConfigRepository;
import com.autowash.features.user.repository.UserRepository;
import com.autowash.features.wallet.entity.Wallet;
import com.autowash.features.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final TierConfigRepository tierConfigRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final WalletRepository walletRepository;

    @Transactional
    public void sendRegistrationOtp(String email) {
        otpService.sendRegistrationOtp(email);
    }

    @Transactional
    public void verifyRegistrationOtp(String email, String otp) {
        otpService.verifyRegistrationOtp(email, otp);
    }

    @Transactional
    public void sendForgotPasswordOtp(String email) {
        otpService.sendForgotPasswordOtp(email);
    }

    @Transactional
    public void verifyForgotPasswordOtp(String email, String otp) {
        otpService.verifyForgotPasswordOtp(email, otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = normalizeRequired(request.getEmail(), "Email is required").toLowerCase();
        String otp = normalizeRequired(request.getOtp(), "OTP is required");
        String newPassword = normalizeRequired(request.getNewPassword(), "New password is required");

        if (newPassword.length() < 6) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu phải chứa ít nhất 6 ký tự"
            );
        }

        otpService.validateForgotPasswordOtpForReset(email, otp);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Email không tồn tại trong hệ thống"
                ));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedAttempt(0);
        user.setLockTime(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);

        otpService.consumeForgotPasswordOtp(email);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeRequired(request.getEmail(), "Email is required").toLowerCase();

        otpService.validateRegistrationOtpForSignup(email, request.getOtp());

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email already exists"
            );
        }

        String fullName = normalizeRequired(request.getFullName(), "Full name is required");
        String username = normalizeRequired(request.getUsername(), "Username is required");
        String phone = normalizeRequired(request.getPhone(), "Phone is required");
        String password = normalizeRequired(request.getPassword(), "Password is required");

        if (password.length() < 6) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must be at least 6 characters"
            );
        }

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username already exists"
            );
        }

        if (userRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone already exists"
            );
        }
        User user = User.builder()
                .fullName(fullName)
                .username(username)
                .email(email)
                .phone(phone)
                .password(passwordEncoder.encode(password))
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .failedAttempt(0)
                .build();

        User savedUser = userRepository.save(user);

        if (savedUser.getRole() == Role.CUSTOMER) {
            TierConfig memberTier = tierConfigRepository.findById(TierLevel.MEMBER)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Default MEMBER tier config not found"
                    ));

            CustomerProfile profile = CustomerProfile.builder()
                    .user(savedUser)
                    .tierConfig(memberTier)
                    .rewardPoints(0)
                    .tierPoints(0)
                    .build();

            customerProfileRepository.save(profile);

            Wallet wallet = Wallet.builder()
                    .user(savedUser)
                    .balance(0)
                    .build();
            walletRepository.save(wallet);
        }

        otpService.consumeRegistrationOtp(email);

        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return new AuthResponse(
                token,
                savedUser.getRole().name(),
                getDashboardUrlByRole(savedUser.getRole()),
                buildAuthUserResponse(savedUser)
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {

        if (request.getUsernameOrPhone() == null || request.getUsernameOrPhone().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng nhập Tên đăng nhập hoặc Số điện thoại"
            );
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng nhập Mật khẩu"
            );
        }

        String usernameOrPhone = normalizeRequired(
                request.getUsernameOrPhone(),
                "Vui lòng nhập Tên đăng nhập hoặc Số điện thoại"
        );

        User user = userRepository.findByUsernameOrPhone(
                usernameOrPhone,
                usernameOrPhone
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Tên đăng nhập/số điện thoại hoặc mật khẩu không chính xác"
        ));

        // 1. Check temporary lock time (5 minutes)
        if (user.getLockTime() != null) {
            if (user.getLockTime().isAfter(LocalDateTime.now())) {
                long remainingSeconds = Duration.between(LocalDateTime.now(), user.getLockTime()).getSeconds();
                long minutes = remainingSeconds / 60;
                long seconds = remainingSeconds % 60;
                String timeMsg = minutes > 0
                        ? (minutes + " phút " + (seconds > 0 ? seconds + " giây" : ""))
                        : (seconds + " giây");

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tài khoản đang bị tạm khóa do nhập sai mật khẩu 5 lần liên tiếp. Vui lòng thử lại sau " + timeMsg + "."
                );
            } else {
                // Lock expired -> auto unlock
                user.setLockTime(null);
                user.setFailedAttempt(0);
                if (user.getStatus() == UserStatus.LOCKED) {
                    user.setStatus(UserStatus.ACTIVE);
                }
                userRepository.save(user);
            }
        }

        // 2. Check general user status
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản của bạn đã bị khóa hoặc vô hiệu hóa. Vui lòng liên hệ quản trị viên."
            );
        }

        // 3. Password match check
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int currentFailed = (user.getFailedAttempt() != null ? user.getFailedAttempt() : 0) + 1;
            user.setFailedAttempt(currentFailed);

            if (currentFailed >= 5) {
                user.setLockTime(LocalDateTime.now().plusMinutes(5));
                user.setStatus(UserStatus.LOCKED);
                userRepository.save(user);

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Bạn đã nhập sai mật khẩu 5 lần liên tiếp. Tài khoản đã bị tạm khóa trong 5 phút."
                );
            } else {
                userRepository.save(user);
                int remaining = 5 - currentFailed;

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Tên đăng nhập hoặc mật khẩu không chính xác. Bạn còn " + remaining + " lần thử."
                );
            }
        }

        // 4. Login successful -> reset counters
        if ((user.getFailedAttempt() != null && user.getFailedAttempt() > 0) || user.getLockTime() != null) {
            user.setFailedAttempt(0);
            user.setLockTime(null);
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getRole().name(),
                getDashboardUrlByRole(user.getRole()),
                buildAuthUserResponse(user)
        );
    }

    private AuthResponse.AuthUserResponse buildAuthUserResponse(User user) {
        String tier = "Member";
        Integer points = 0;
        if (user.getRole() == Role.CUSTOMER) {
            CustomerProfile profile = customerProfileRepository.findByUser(user).orElse(null);
            if (profile != null) {
                tier = (profile.getTierConfig() != null) ? profile.getTierConfig().getTierLevel().name() : "Member";
                points = profile.getRewardPoints();
            }
        } else if (user.getRole() == Role.ADMIN) {
            tier = "Admin";
        } else if (user.getRole() == Role.STAFF) {
            tier = "Staff";
        }
        
        Integer walletBalance = walletRepository.findWalletByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(0);

        String formattedTier = tier;
        if ("MEMBER".equalsIgnoreCase(tier)) formattedTier = "Member";
        else if ("SILVER".equalsIgnoreCase(tier)) formattedTier = "Silver";
        else if ("GOLD".equalsIgnoreCase(tier)) formattedTier = "Gold";
        else if ("PLATINUM".equalsIgnoreCase(tier)) formattedTier = "Platinum";

        return AuthResponse.AuthUserResponse.builder()
                .id(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .tier(formattedTier)
                .points(points)
                .walletBalance(walletBalance)
                .build();
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return value.trim();
    }

    private String getDashboardUrlByRole(Role role) {
        if (role == Role.ADMIN) {
            return "/admin/dashboard";
        }

        if (role == Role.STAFF) {
            return "/staff/dashboard";
        }

        return "/customer/dashboard";
    }
}


