package com.autowash.features.promotion.dto;

import com.autowash.features.promotion.entity.Promotion;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter

public class TopUsedVoucherResponse {
    private Long promotionId;
    private String voucherCode;
    private String campaignName;
    private Integer discountAmount;
    private BigDecimal discountPercent;
    private Integer maxDiscountAmount;
    private Long usedCount;
}

