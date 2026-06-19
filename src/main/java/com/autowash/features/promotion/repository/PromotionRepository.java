package com.autowash.features.promotion.repository;

import com.autowash.features.booking.entity.Booking;
import com.autowash.features.promotion.entity.CustomerVoucher;

import com.autowash.features.booking.entity.Payment;
import com.autowash.features.booking.enums.PaymentStatus;

import com.autowash.features.promotion.dto.TopUsedVoucherResponse;
import com.autowash.features.promotion.dto.VoucherResponse;
import com.autowash.features.promotion.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("""
        SELECT new com.autowash.dto.response.TopUsedVoucherResponse(
            promo.id,  
            promo.voucherCode,
            promo.campaignName,
            promo.discountAmount,
            promo.discountPercent,
            promo.maxDiscountAmount,
            COUNT(pay)
        ) 
        FROM Promotion promo
        LEFT JOIN CustomerVoucher cv ON cv.promotion = promo
        LEFT JOIN Payment pay ON pay.appliedVoucher = cv AND pay.paymentStatus = com.autowash.enums.PaymentStatus.PAID
        GROUP BY 
            promo.id,  
            promo.voucherCode,
            promo.campaignName,
            promo.discountAmount,
            promo.discountPercent,
            promo.maxDiscountAmount
        ORDER BY COUNT(pay) DESC, promo.id DESC
    """)
    List<TopUsedVoucherResponse> findTopVoucher();
}


