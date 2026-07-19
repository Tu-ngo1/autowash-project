package com.autowash.features.user.dto.response;

import com.autowash.features.user.enums.TierLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TierConfigResponse {
    private Long id;
    private String name;
    private String description;
    private Integer pointsRequired;
    private Double discountPercent;
    private Long customerCount;

    // Custom constructor for JPQL query mapping
    public TierConfigResponse(TierLevel tierLevel, Integer pointsRequired, BigDecimal discountPercent, Long customerCount) {
        long tempId = 1L;
        if (tierLevel == TierLevel.SILVER) tempId = 2L;
        if (tierLevel == TierLevel.GOLD) tempId = 3L;
        if (tierLevel == TierLevel.PLATINUM) tempId = 4L;
        
        this.id = tempId;
        this.name = tierLevel.name();
        switch (tierLevel) {
            case SILVER -> {
                this.description = "Ưu đãi hạng Bạc";
            }
            case GOLD -> {
                this.description = "Ưu đãi hạng Vàng";
            }
            case PLATINUM -> {
                this.description = "Ưu đãi hạng Bạch kim";
            }
            default -> {
                this.description = "Ưu đãi hạng Thành viên";
            }
        }
        this.pointsRequired = pointsRequired;
        this.discountPercent = discountPercent != null ? discountPercent.doubleValue() : 0.0;
        this.customerCount = customerCount;
    }
}



