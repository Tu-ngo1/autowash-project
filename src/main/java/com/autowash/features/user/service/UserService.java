package com.autowash.features.user.service;

import com.autowash.features.car.entity.Car;

import com.autowash.features.booking.entity.Booking;

import com.autowash.features.user.dto.CreateStaffRequest;
import com.autowash.features.user.dto.UpdateProfileRequest;
import com.autowash.features.user.dto.ProfileResponse;
import com.autowash.features.user.dto.UserResponse;
import com.autowash.features.user.entity.CustomerProfile;
import com.autowash.features.user.entity.User;
import com.autowash.features.car.enums.CarStatus;
import com.autowash.features.user.enums.Role;
import com.autowash.features.user.enums.UserStatus;
import com.autowash.features.user.mapper.ProfileMapper;
import com.autowash.features.user.mapper.UserMapper;
import com.autowash.features.booking.repository.BookingRepository;
import com.autowash.features.car.repository.CarRepository;
import com.autowash.features.user.repository.CustomerProfileRepository;
import com.autowash.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import com.autowash.features.user.dto.AdminUserResponse;
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserMapper userMapper;
    private final ProfileMapper profileMapper;
    private final PasswordEncoder passwordEncoder;
    private final CarRepository carRepository;
    private final BookingRepository bookingRepository;

    public User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User is not authenticated"
            );
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));
    }

    public UserResponse getCurrentUser() {
        User user = getCurrentUserEntity();
        return userMapper.toResponse(user);
    }

    public ProfileResponse getCurrentUserProfile() {
        User user = getCurrentUserEntity();

        CustomerProfile profile = customerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer profile not found"
                ));

        return profileMapper.toResponse(profile);
    }

    @Transactional
    public UserResponse updateCurrentUserProfile(UpdateProfileRequest request) {
        User user = getCurrentUserEntity();

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findByRoleNot(Role.ADMIN)
                .stream()
                .map(user -> {
                    Long userId = user.getId();
                    int carCount = carRepository.countByUserId(userId);
                    int bookingCount = bookingRepository.countByUserId(userId);
                    return userMapper.toAdminUserResponse(user, carCount, bookingCount);
                })
                .toList();
    }

    @Transactional
    public UserResponse createStaff(CreateStaffRequest request) {
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Full name is required"
            );
        }

        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone is required"
            );
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must be at least 6 characters"
            );
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone already exists"
            );
        }

        if (request.getUsername() != null
                && !request.getUsername().isBlank()
                && userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username already exists"
            );
        }

        if (request.getEmail() != null
                && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email already exists"
            );
        }

        User staff = User.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STAFF)
                .status(UserStatus.ACTIVE)
                .build();

        User savedStaff = userRepository.save(staff);

        return userMapper.toResponse(savedStaff);
    }

    @Transactional
    public UserResponse lockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        user.setStatus(UserStatus.LOCKED);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponse unlockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }


    public List<AdminUserResponse> getAllCustomersForAdmin() {
        return customerProfileRepository.findAll()
                .stream()
                .map(profile -> {
                    Long userId = profile.getUser().getId();

                    int carCount = carRepository.countByUserIdAndStatus(userId, CarStatus.ACTIVE);
                    int bookingCount = bookingRepository.countByUserId(userId);

                    return userMapper.toAdminUserResponse(
                            profile.getUser(),
                            carCount,
                            bookingCount
                    );
                })
                .toList();
    }
}


