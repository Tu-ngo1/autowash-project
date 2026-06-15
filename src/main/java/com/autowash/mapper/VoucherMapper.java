package com.autowash.mapper;

import com.autowash.dto.response.VoucherResponse;
import com.autowash.entity.Promotion;
import org.springframework.stereotype.Component;

@Component
public class VoucherMapper {

    public VoucherResponse toResponse(Promotion promotion) {
        if (promotion == null) return null;

        String discountType = "fixed";
        Object discountValue = 0;
        if (promotion.getDiscountPercent() != null) {
            discountType = "Percent";
            discountValue = promotion.getDiscountPercent();
        } else if (promotion.getDiscountAmount() != null) {
            discountType = "Fixed";
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

        return new VoucherResponse(
                promotion.getId(),
                promotion.getCampaignName(),
                promotion.getVoucherCode(),
                promotion.getPointCost(),
                targetTier,
                promotion.getStartAt(),
                promotion.getEndAt(),
                promotion.getActive(),
                discountType,
                discountValue
        );
    }
}
