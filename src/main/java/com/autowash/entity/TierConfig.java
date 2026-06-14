package com.autowash.entity;

import com.autowash.enums.TierLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "TIER_CONFIGS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TierConfig {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "Tier_level")
    @EqualsAndHashCode.Include
    private TierLevel tierLevel;

    @Column(name = "Points_to_upgrade", nullable = false)
    private Integer pointsToUpgrade;

    @Column(name = "Points_to_maintain", nullable = false)
    private Integer pointsToMaintain;

    @Column(name = "Booking_window_days", nullable = false)
    private Integer bookingWindowDays;

    @Column(name = "Auto_discount_percent", precision = 5, scale = 2)
    private BigDecimal autoDiscountPercent;

    @Builder.Default
    @Column(name = "Is_active", nullable = false)
    private Boolean active = true;
}
