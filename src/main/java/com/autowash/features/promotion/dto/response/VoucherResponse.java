package com.autowash.features.promotion.dto.response;

import com.autowash.features.promotion.entity.Promotion;

import com.autowash.features.user.entity.User;

import com.autowash.features.user.enums.TierLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherResponse {
    private Long id;
    private String name;
    private String code;
    private Integer pointsRequired;
    private String tier;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private String discountType;
    private Object discountValue; // Có thể dùng Double/Integer hoặc BigDecimal
    private Integer maxDiscountAmount;
}


