package com.autowash.features.user.controller;

import com.autowash.features.washservice.entity.Service;
import com.autowash.features.user.entity.User;
import com.autowash.features.user.dto.response.ProfileResponse;
import com.autowash.features.user.dto.response.UserResponse;
import com.autowash.features.user.service.UserService;
import com.autowash.features.booking.entity.DailyOperationsConfig;
import com.autowash.features.booking.repository.DailyOperationsConfigRepository;
import com.autowash.features.promotion.entity.CustomerVoucher;
import com.autowash.features.promotion.entity.Promotion;
import com.autowash.features.promotion.repository.CustomerVoucherRepository;
import com.autowash.features.promotion.repository.PromotionRepository;
import com.autowash.features.user.entity.TierConfig;
import com.autowash.features.user.entity.CustomerProfile;
import com.autowash.features.user.repository.TierConfigRepository;
import com.autowash.features.user.enums.TierLevel;
import com.autowash.features.user.dto.request.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final UserService userService;
    private final DailyOperationsConfigRepository dailyOperationsConfigRepository;
    private final TierConfigRepository tierConfigRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final PromotionRepository promotionRepository;

    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.getCurrentUser();
    }

    @GetMapping("/profile")
    public ProfileResponse getCurrentUserProfile() {
        return userService.getCurrentUserProfile();
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(@RequestBody UpdateProfileRequest request) {
        return userService.updateCurrentUserProfile(request);
    }

    // --- Endpoints cấu hình đặt lịch ---

    @GetMapping("/booking-config")
    public BookingConfigResponse getBookingConfig() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        DailyOperationsConfig config = dailyOperationsConfigRepository.findByConfigDate(tomorrow)
                .orElse(DailyOperationsConfig.builder()
                        .configDate(tomorrow)
                        .openTime(LocalTime.of(8, 0))
                        .closeTime(LocalTime.of(18, 0))
                        .bayCount(2)
                        .isActive(true)
                        .build());

        String startTimeStr = config.getOpenTime() != null ? config.getOpenTime().toString().substring(0, 5) : "08:00";
        String endTimeStr = config.getCloseTime() != null ? config.getCloseTime().toString().substring(0, 5) : "18:00";

        List<TierConfig> tierConfigs = tierConfigRepository.findAll();
        List<TierRuleResponse> tierRules = tierConfigs.stream()
                .map(tc -> TierRuleResponse.builder()
                        .tierLevel(tc.getTierLevel().name())
                        .discountPercent(tc.getAutoDiscountPercent() != null ? tc.getAutoDiscountPercent().doubleValue() : 0.0)
                        .advanceBookingDays(tc.getBookingWindowDays() != null ? tc.getBookingWindowDays() : 7)
                        .build())
                .collect(Collectors.toList());

        return BookingConfigResponse.builder()
                .businessHours(BusinessHoursResponse.builder()
                        .startTime(startTimeStr)
                        .endTime(endTimeStr)
                        .build())
                .slotDurationMinutes(60) // Theo hợp đồng API yêu cầu 60 phút
                .tierRules(tierRules)
                .build();
    }

    @GetMapping("/tier-configs")
    public List<CustomerTierConfigResponse> getTierConfigs() {
        List<TierConfig> configs = tierConfigRepository.findAll();
        List<CustomerTierConfigResponse> responses = new ArrayList<>();
        
        // Sắp xếp theo thứ tự phân hạng để gán id hợp lý
        // MEMBER, SILVER, GOLD, PLATINUM
        for (TierConfig tc : configs) {
            long id = 1;
            String label = tc.getTierLevel().name();
            int minPoints = tc.getPointsToMaintain() != null ? tc.getPointsToMaintain() : 0;
            
            switch (tc.getTierLevel()) {
                case MEMBER:
                    id = 1;
                    label = "Thành viên";
                    minPoints = 0;
                    break;
                case SILVER:
                    id = 2;
                    label = "Bạc";
                    minPoints = 1000;
                    break;
                case GOLD:
                    id = 3;
                    label = "Vàng";
                    minPoints = 3000;
                    break;
                case PLATINUM:
                    id = 4;
                    label = "Bạch kim";
                    minPoints = 6000;
                    break;
            }
            
            responses.add(CustomerTierConfigResponse.builder()
                    .id(id)
                    .tierLevel(tc.getTierLevel().name())
                    .label(label)
                    .minPoints(minPoints)
                    .build());
        }
        
        return responses;
    }

    @GetMapping("/loyalty")
    public LoyaltyResponse getLoyaltySummary() {
        User currentUser = userService.getCurrentUserEntity();
        CustomerProfile profile = currentUser.getCustomerProfile();
        
        String tierName = "MEMBER";
        int points = 0;
        int redeemablePoints = 0;
        
        if (profile != null) {
            tierName = profile.getTierConfig() != null ? profile.getTierConfig().getTierLevel().name() : "MEMBER";
            points = profile.getTierPoints() != null ? profile.getTierPoints() : 0;
            redeemablePoints = profile.getRewardPoints() != null ? profile.getRewardPoints() : 0;
        }
        
        List<TierConfig> tierConfigs = tierConfigRepository.findAll();
        TierConfig currentTier = null;
        TierConfig nextTier = null;
        
        for (TierConfig tc : tierConfigs) {
            if (tc.getTierLevel().name().equalsIgnoreCase(tierName)) {
                currentTier = tc;
            }
        }
        
        if (tierName.equalsIgnoreCase("MEMBER")) {
            nextTier = findTierConfigByLevel(tierConfigs, TierLevel.SILVER);
        } else if (tierName.equalsIgnoreCase("SILVER")) {
            nextTier = findTierConfigByLevel(tierConfigs, TierLevel.GOLD);
        } else if (tierName.equalsIgnoreCase("GOLD")) {
            nextTier = findTierConfigByLevel(tierConfigs, TierLevel.PLATINUM);
        }
        
        int nextTierTarget = points;
        int progress = 100;
        
        if (nextTier != null) {
            int currentMin = currentTier != null && currentTier.getPointsToMaintain() != null ? currentTier.getPointsToMaintain() : 0;
            int targetPoints = nextTier.getPointsToMaintain() != null ? nextTier.getPointsToMaintain() : 0;
            nextTierTarget = targetPoints;
            if (targetPoints > currentMin) {
                progress = ((points - currentMin) * 100) / (targetPoints - currentMin);
                if (progress < 0) progress = 0;
                if (progress > 100) progress = 100;
            } else {
                progress = 0;
            }
        }
        
        List<CustomerVoucher> cvs = customerVoucherRepository.findByUserId(currentUser.getId());
        List<CustomerVoucherResponse> vouchers = cvs.stream()
                .map(this::mapToCustomerVoucherResponse)
                .collect(Collectors.toList());
        
        return LoyaltyResponse.builder()
                .tier(tierName)
                .points(points)
                .redeemablePoints(redeemablePoints)
                .nextTierTarget(nextTierTarget)
                .progress(progress)
                .vouchers(vouchers)
                .build();
    }
    
    private TierConfig findTierConfigByLevel(List<TierConfig> configs, TierLevel level) {
        return configs.stream()
                .filter(c -> c.getTierLevel() == level)
                .findFirst()
                .orElse(null);
    }

    @PostMapping("/loyalty/redeem")
    public CustomerVoucherResponse redeemVoucher(@RequestBody RedeemVoucherRequest request) {
        User currentUser = userService.getCurrentUserEntity();
        return userService.redeemVoucher(currentUser.getId(), request.getVoucherId());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedeemVoucherRequest {
        private Long voucherId;
    }

    // --- Endpoints quản lý Voucher cho khách hàng ---

    @GetMapping("/loyalty/vouchers")
    public List<CustomerVoucherResponse> getLoyaltyVouchers() {
        User currentUser = userService.getCurrentUserEntity();
        CustomerProfile profile = currentUser.getCustomerProfile();
        
        TierLevel userTier = (profile != null && profile.getTierConfig() != null) 
                ? profile.getTierConfig().getTierLevel() 
                : TierLevel.MEMBER;
                
        List<Promotion> promotions = promotionRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        
        return promotions.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> p.getStartAt() == null || p.getStartAt().isBefore(now))
                .filter(p -> p.getEndAt() == null || p.getEndAt().isAfter(now))
                .filter(p -> p.getTargetTier() != null && userTier.ordinal() >= p.getTargetTier().getTierLevel().ordinal())
                .map(this::mapPromotionToResponse)
                .collect(Collectors.toList());
    }

    private CustomerVoucherResponse mapPromotionToResponse(Promotion promotion) {
        return CustomerVoucherResponse.builder()
                .id(null)
                .promotionId(promotion.getId())
                .voucherCode(promotion.getVoucherCode())
                .campaignName(promotion.getCampaignName())
                .discountAmount(promotion.getDiscountAmount())
                .discountPercent(promotion.getDiscountPercent())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .pointCost(promotion.getPointCost())
                .status("AVAILABLE")
                .build();
    }

    @GetMapping("/{customerId}/vouchers")
    public List<CustomerVoucherResponse> getCustomerVouchers(@PathVariable Long customerId) {
        User currentUser = userService.getCurrentUserEntity();
        
        // Bảo vệ tài nguyên, chỉ cho phép tự truy vấn voucher của mình
        if (!currentUser.getId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền xem voucher của khách hàng này"
            );
        }
        
        List<CustomerVoucher> cvs = customerVoucherRepository.findByUserId(customerId);
        return cvs.stream()
                .map(this::mapToCustomerVoucherResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/vouchers/validate")
    public ValidateVoucherResponse validateVoucher(@RequestBody ValidateVoucherRequest request) {
        User currentUser = userService.getCurrentUserEntity();
        if (request.getCode() == null || request.getCode().trim().isBlank()) {
            return ValidateVoucherResponse.builder().valid(false).build();
        }
        
        List<CustomerVoucher> userVouchers = customerVoucherRepository.findByUserId(currentUser.getId());
        
        CustomerVoucher target = userVouchers.stream()
                .filter(cv -> cv.getVoucherCode().equalsIgnoreCase(request.getCode().trim()))
                .findFirst()
                .orElse(null);
                
        if (target == null) {
            return ValidateVoucherResponse.builder().valid(false).build();
        }
        
        boolean isAvailable = target.getStatus() == com.autowash.features.promotion.enums.VoucherStatus.AVAILABLE;
        boolean isNotExpired = target.getExpiredAt() == null || target.getExpiredAt().isAfter(LocalDateTime.now());
        
        if (!isAvailable || !isNotExpired) {
            return ValidateVoucherResponse.builder().valid(false).build();
        }
        
        Promotion promo = target.getPromotion();
        return ValidateVoucherResponse.builder()
                .valid(true)
                .discountAmount(promo != null ? promo.getDiscountAmount() : null)
                .discountPercent(promo != null ? promo.getDiscountPercent() : null)
                .maxDiscountAmount(promo != null ? promo.getMaxDiscountAmount() : null)
                .voucher(VoucherBriefResponse.builder()
                        .id(target.getId())
                        .voucherCode(target.getVoucherCode())
                        .build())
                .build();
    }

    private CustomerVoucherResponse mapToCustomerVoucherResponse(CustomerVoucher cv) {
        Promotion promo = cv.getPromotion();
        return CustomerVoucherResponse.builder()
                .id(cv.getId())
                .promotionId(promo != null ? promo.getId() : null)
                .voucherCode(cv.getVoucherCode())
                .campaignName(promo != null ? promo.getCampaignName() : "")
                .discountAmount(promo != null ? promo.getDiscountAmount() : null)
                .discountPercent(promo != null ? promo.getDiscountPercent() : null)
                .maxDiscountAmount(promo != null ? promo.getMaxDiscountAmount() : null)
                .pointCost(promo != null ? promo.getPointCost() : null)
                .status(cv.getStatus() != null ? cv.getStatus().name() : "AVAILABLE")
                .redeemedAt(cv.getRedeemedAt())
                .usedAt(cv.getUsedAt())
                .expiredAt(cv.getExpiredAt())
                .build();
    }

    // --- Cấu trúc DTO nội bộ ---

    @Getter
    @Builder
    public static class BookingConfigResponse {
        private BusinessHoursResponse businessHours;
        private int slotDurationMinutes;
        private List<TierRuleResponse> tierRules;
    }

    @Getter
    @Builder
    public static class BusinessHoursResponse {
        private String startTime;
        private String endTime;
    }

    @Getter
    @Builder
    public static class TierRuleResponse {
        private String tierLevel;
        private double discountPercent;
        private int advanceBookingDays;
    }

    @Getter
    @Builder
    public static class CustomerTierConfigResponse {
        private Long id;
        private String tierLevel;
        private String label;
        private int minPoints;
    }

    @Getter
    @Builder
    public static class CustomerVoucherResponse {
        private Long id;
        private Long promotionId;
        private String voucherCode;
        private String campaignName;
        private Integer discountAmount;
        private BigDecimal discountPercent;
        private Integer maxDiscountAmount;
        private Integer pointCost;
        private String status;
        private LocalDateTime redeemedAt;
        private LocalDateTime usedAt;
        private LocalDateTime expiredAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateVoucherRequest {
        private String code;
    }

    @Getter
    @Builder
    public static class ValidateVoucherResponse {
        private boolean valid;
        private Integer discountAmount;
        private BigDecimal discountPercent;
        private Integer maxDiscountAmount;
        private VoucherBriefResponse voucher;
    }

    @Getter
    @Builder
    public static class VoucherBriefResponse {
        private Long id;
        private String voucherCode;
    }

    @Getter
    @Builder
    public static class LoyaltyResponse {
        private String tier;
        private Integer points;
        private Integer redeemablePoints;
        private Integer nextTierTarget;
        private Integer progress;
        private List<CustomerVoucherResponse> vouchers;
    }
}

