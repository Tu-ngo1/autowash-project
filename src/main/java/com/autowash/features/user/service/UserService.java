package com.autowash.features.user.service;

import com.autowash.features.car.entity.Car;
import com.autowash.features.booking.entity.Booking;
import com.autowash.features.user.dto.request.CreateStaffRequest;
import com.autowash.features.user.dto.request.UpdateProfileRequest;
import com.autowash.features.user.dto.request.UpdateUserByAdminRequest;
import com.autowash.features.user.dto.request.UpdateUserPointsRequest;
import com.autowash.features.user.dto.response.ProfileResponse;
import com.autowash.features.user.dto.response.UserResponse;
import com.autowash.features.user.dto.response.AdminUserDetailResponse;
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
import com.autowash.features.wallet.repository.WalletRepository;
import com.autowash.features.wallet.entity.Wallet;
import com.autowash.features.user.repository.TierConfigRepository;
import com.autowash.features.user.entity.TierConfig;
import com.autowash.features.user.enums.TierLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import com.autowash.features.user.dto.response.AdminUserResponse;
import com.autowash.features.user.dto.response.CustomerSearchResponse;
import com.autowash.features.car.dto.response.CarResponse;
import java.util.Optional;
import com.autowash.features.promotion.entity.Promotion;
import com.autowash.features.promotion.entity.CustomerVoucher;
import com.autowash.features.promotion.enums.VoucherStatus;
import com.autowash.features.promotion.repository.PromotionRepository;
import com.autowash.features.promotion.repository.CustomerVoucherRepository;
import com.autowash.features.user.controller.CustomerController.CustomerVoucherResponse;
import java.util.UUID;
import java.time.LocalDateTime;

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
    private final PromotionRepository promotionRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final WalletRepository walletRepository;
    private final TierConfigRepository tierConfigRepository;

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
                    int carCount = carRepository.countByUserIdAndStatus(userId, CarStatus.ACTIVE);
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

    public CustomerSearchResponse searchCustomer(String query) {
        if (query == null || query.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Từ khóa tìm kiếm không được để trống");
        }
        
        String cleanQuery = query.trim();
        Optional<User> userOpt = userRepository.findByPhone(cleanQuery);
        
        if (userOpt.isEmpty()) {
            // Nếu không tìm thấy bằng SĐT, tìm bằng biển số xe
            userOpt = carRepository.findByLicensePlate(cleanQuery)
                    .map(Car::getUser);
        }
        
        User user = userOpt.orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy khách hàng hoặc xe phù hợp"
        ));
        
        String tierLevel = "MEMBER";
        int rewardPoints = 0;
        if (user.getCustomerProfile() != null) {
            rewardPoints = user.getCustomerProfile().getRewardPoints();
            if (user.getCustomerProfile().getTierConfig() != null) {
                tierLevel = user.getCustomerProfile().getTierConfig().getTierLevel().name();
            }
        }
        
        List<CarResponse> registeredVehicles = carRepository.findByUserId(user.getId())
                .stream()
                .map(CarResponse::fromCar)
                .toList();
                
        return CustomerSearchResponse.builder()
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .tierLevel(tierLevel)
                .rewardPoints(rewardPoints)
                .registeredVehicles(registeredVehicles)
                .build();
    }

    @Transactional
    public CustomerVoucherResponse redeemVoucher(Long userId, Long promotionId) {
        // 1. Tìm thông tin khách hàng và profile
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
                
        CustomerProfile profile = customerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ thành viên"));

        // 2. Tìm thông tin chiến dịch khuyến mãi (Promotion)
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chương trình khuyến mại"));

        // 3. Kiểm tra xem Promotion có đang hoạt động không
        if (Boolean.FALSE.equals(promotion.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chương trình khuyến mại đã ngừng hoạt động");
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartAt() != null && promotion.getStartAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chương trình khuyến mại chưa bắt đầu");
        }
        if (promotion.getEndAt() != null && promotion.getEndAt().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chương trình khuyến mại đã kết thúc");
        }

        // 4. Kiểm tra số dư điểm thưởng
        int pointsNeeded = promotion.getPointCost() != null ? promotion.getPointCost() : 0;
        int currentPoints = profile.getRewardPoints() != null ? profile.getRewardPoints() : 0;
        
        if (currentPoints < pointsNeeded) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điểm thưởng tích lũy không đủ để đổi voucher này");
        }

        // 5. Khấu trừ điểm thưởng
        profile.setRewardPoints(currentPoints - pointsNeeded);
        customerProfileRepository.save(profile);

        // 6. Sinh mã Voucher ngẫu nhiên duy nhất cho khách hàng
        // Định dạng: [MÃ_CHIẾN_DỊCH]-[RNG-4-CHAR] (ví dụ: GOLD10-8D2A)
        String uniqueVoucherCode = promotion.getVoucherCode() + "-" + 
                UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        // 7. Tạo mới CustomerVoucher
        LocalDateTime expiredDate = promotion.getEndAt() != null ? promotion.getEndAt() : now.plusDays(30);
        CustomerVoucher customerVoucher = CustomerVoucher.builder()
                .user(user)
                .promotion(promotion)
                .voucherCode(uniqueVoucherCode)
                .status(VoucherStatus.AVAILABLE)
                .redeemedAt(now)
                .expiredAt(expiredDate)
                .build();

        CustomerVoucher savedVoucher = customerVoucherRepository.save(customerVoucher);

        // 8. Trả về Response DTO tương thích với Frontend
        CustomerVoucherResponse response = CustomerVoucherResponse.builder()
                .id(savedVoucher.getId())
                .promotionId(promotion.getId())
                .voucherCode(savedVoucher.getVoucherCode())
                .campaignName(promotion.getCampaignName())
                .discountAmount(promotion.getDiscountAmount())
                .discountPercent(promotion.getDiscountPercent())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .pointCost(promotion.getPointCost())
                .status(savedVoucher.getStatus().name())
                .redeemedAt(savedVoucher.getRedeemedAt())
                .expiredAt(savedVoucher.getExpiredAt())
                .build();
        return response;
    }

    public User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getAdminUserDetail(Long userId) {
        User user = getUserEntityById(userId);

        CustomerProfile profile = user.getCustomerProfile();
        String tier = profile != null && profile.getTierConfig() != null ? profile.getTierConfig().getTierLevel().name() : "MEMBER";
        int tPoints = profile != null && profile.getTierPoints() != null ? profile.getTierPoints() : 0;
        int rPoints = profile != null && profile.getRewardPoints() != null ? profile.getRewardPoints() : 0;

        int walletBalance = walletRepository.findWalletByUserId(userId)
                .map(Wallet::getBalance)
                .orElse(0);

        List<CarResponse> vehicles = carRepository.findByUserIdAndStatus(userId, CarStatus.ACTIVE).stream()
                .map(CarResponse::fromCar)
                .toList();

        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .username(user.getUsername())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .tier(tier)
                .tierPoints(tPoints)
                .rewardPoints(rPoints)
                .walletBalance(walletBalance)
                .vehicles(vehicles)
                .build();
    }

    @Transactional
    public UserResponse updateUserByAdmin(Long userId, UpdateUserByAdminRequest request) {
        User user = getUserEntityById(userId);

        if (request.getFullName() == null || request.getFullName().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ tên không được để trống");
        }
        if (request.getPhone() == null || request.getPhone().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không được để trống");
        }

        String email = request.getEmail() != null ? request.getEmail().trim() : null;
        String phone = request.getPhone().trim();

        if (email != null && !email.isBlank() && !email.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã tồn tại trong hệ thống");
            }
            user.setEmail(email);
        }

        if (!phone.equals(user.getPhone())) {
            if (userRepository.existsByPhone(phone)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại đã tồn tại trong hệ thống");
            }
            user.setPhone(phone);
        }

        user.setFullName(request.getFullName().trim());
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponse updatePointsAndRecalculateTier(Long userId, UpdateUserPointsRequest request) {
        User user = getUserEntityById(userId);

        if (user.getRole() != Role.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Người dùng này không phải là khách hàng");
        }

        CustomerProfile profile = customerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ của khách hàng"));

        if (request.getRewardPoints() != null) {
            profile.setRewardPoints(request.getRewardPoints());
        } else if (request.getRedeemPointsDelta() != null) {
            int currentRewardPoints = profile.getRewardPoints() != null ? profile.getRewardPoints() : 0;
            profile.setRewardPoints(Math.max(0, currentRewardPoints + request.getRedeemPointsDelta()));
        }

        Integer newTierPoints = null;
        if (request.getTierPoints() != null) {
            newTierPoints = request.getTierPoints();
        } else if (request.getRankPointsDelta() != null) {
            int currentTierPoints = profile.getTierPoints() != null ? profile.getTierPoints() : 0;
            newTierPoints = Math.max(0, currentTierPoints + request.getRankPointsDelta());
        }

        if (newTierPoints != null) {
            profile.setTierPoints(newTierPoints);

            final int finalPoints = newTierPoints;
            List<TierConfig> configs = tierConfigRepository.findAll();
            
            TierConfig newTierConfig = configs.stream()
                    .filter(tc -> tc.getActive() != null && tc.getActive())
                    .filter(tc -> tc.getPointsToMaintain() != null && finalPoints >= tc.getPointsToMaintain())
                    .max((tc1, tc2) -> Integer.compare(
                            tc1.getPointsToMaintain() != null ? tc1.getPointsToMaintain() : 0,
                            tc2.getPointsToMaintain() != null ? tc2.getPointsToMaintain() : 0
                    ))
                    .orElseGet(() -> configs.stream()
                            .filter(tc -> tc.getTierLevel() == TierLevel.MEMBER)
                            .findFirst()
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy cấu hình hạng MEMBER")));
                            
            profile.setTierConfig(newTierConfig);
        }

        customerProfileRepository.save(profile);
        return userMapper.toResponse(user);
    }
}


