package com.autowash.features.user.dto;

import com.autowash.features.user.entity.User;

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
    private String id;
    private String name;
    private String description;
    private Integer pointsRequired;
    private BigDecimal discountPercent;
    private Long customerCount;

    // Custom constructor for JPQL query mapping
    public TierConfigResponse(TierLevel tierLevel, Integer pointsRequired, BigDecimal discountPercent, Long customerCount) {
        this.id = tierLevel.name();
        switch (tierLevel) {
            case SILVER -> {
                this.name = "Silver";
                this.description = "Ưu đãi hạng Bạc";
            }
            case GOLD -> {
                this.name = "Gold";
                this.description = "Ưu đãi hạng Vàng";
            }
            case PLATINUM -> {
                this.name = "Platinum";
                this.description = "Ưu đãi hạng Bạch kim";
            }
            default -> {
                this.name = "Member";
                this.description = "Ưu đãi hạng Thành viên";
            }
        }
        this.pointsRequired = pointsRequired;
        this.discountPercent = discountPercent;
        this.customerCount = customerCount;
    }
}



