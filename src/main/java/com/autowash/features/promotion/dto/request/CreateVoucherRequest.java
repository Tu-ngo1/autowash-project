package com.autowash.features.promotion.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateVoucherRequest {
    private String voucherCode;
    private String campaignName;
    private Integer pointCost;
    private String targetTier;
    private Integer discountAmount;
    private BigDecimal discountPercent;
    private Integer maxDiscountAmount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
