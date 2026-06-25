package com.autowash.features.auth.service;



import com.autowash.features.auth.dto.request.LoginRequest;
import com.autowash.features.auth.dto.request.RegisterRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final TierConfigRepository tierConfigRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    @Transactional
    public void sendRegistrationOtp(String email) {
        otpService.sendRegistrationOtp(email);
    }

    @Transactional
    public void verifyRegistrationOtp(String email, String otp) {
        otpService.verifyRegistrationOtp(email, otp);
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
        }

        otpService.consumeRegistrationOtp(email);

        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return new AuthResponse(
                token,
                savedUser.getRole().name(),
                getDashboardUrlByRole(savedUser.getRole())
        );
    }

    public AuthResponse login(LoginRequest request) {

        if (request.getUsernameOrPhone() == null || request.getUsernameOrPhone().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username or phone is required"
            );
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password is required"
            );
        }

        String emailOrPhone = normalizeRequired(
                request.getUsernameOrPhone(),
                "Email or phone is required"
        );

        User user = userRepository.findByEmailOrPhone(
                emailOrPhone.toLowerCase(),
                emailOrPhone
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials"
        ));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username/phone or password"
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account is locked or disabled"
            );
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(token, user.getRole().name(), getDashboardUrlByRole(user.getRole()));
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


