package com.autowash.features.promotion.mapper;

import com.autowash.features.promotion.dto.response.VoucherResponse;
import com.autowash.features.promotion.entity.Promotion;
import org.springframework.stereotype.Component;

@Component
public class VoucherMapper {

    public VoucherResponse toResponse(Promotion promotion) {
        if (promotion == null) return null;

        String discountType = "fixed";
        Object discountValue = 0;
        if (promotion.getDiscountPercent() != null) {
            discountType = "percentage";
            discountValue = promotion.getDiscountPercent();
        } else if (promotion.getDiscountAmount() != null) {
            discountType = "fixed";
            discountValue = promotion.getDiscountAmount();
        }

        String targetTier = "all";
        if (promotion.getTargetTier() != null) {
            switch (promotion.getTargetTier().getTierLevel()) {
                case SILVER -> targetTier = "Silver";
                case GOLD -> targetTier = "Gold";
                case PLATINUM -> targetTier = "Platinum";
                default -> targetTier = "all";
            }
        }

        return VoucherResponse.builder()
                .id(promotion.getId())
                .name(promotion.getCampaignName())
                .code(promotion.getVoucherCode())
                .pointsRequired(promotion.getPointCost())
                .tier(targetTier)
                .startDate(promotion.getStartAt())
                .endDate(promotion.getEndAt())
                .isActive(promotion.getActive())
                .discountType(discountType)
                .discountValue(discountValue)
                .discountPercent(promotion.getDiscountPercent())
                .discountAmount(promotion.getDiscountAmount())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .build();
    }
}


