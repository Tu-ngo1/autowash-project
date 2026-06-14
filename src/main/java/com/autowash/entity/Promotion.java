package com.autowash.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PROMOTIONS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "Voucher_code", nullable = false, unique = true)
    private String voucherCode;

    @Column(name = "Campaign_name", nullable = false)
    private String campaignName;

    @Column(name = "Point_cost", nullable = false)
    private Integer pointCost;

    @ManyToOne
    @JoinColumn(name = "target_tier", nullable = false)
    private TierConfig targetTier;

    @Column(name = "Discount_amount")
    private Integer discountAmount;

    @Column(name = "Discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "Max_discount_amount")
    private Integer maxDiscountAmount;

    @Builder.Default
    @Column(name = "Is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "Start_at")
    private LocalDateTime startAt;

    @Column(name = "End_at")
    private LocalDateTime endAt;

    @Column(name = "Created_at")
    private LocalDateTime createdAt;

    @Column(name = "Updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
