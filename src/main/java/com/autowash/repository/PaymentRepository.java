package com.autowash.repository;

import com.autowash.dto.response.TopUsedVoucherResponse;
import com.autowash.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
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
        FROM Payment pay
        JOIN pay.appliedVoucher customerVoucher
        JOIN customerVoucher.promotion promo
        WHERE pay.paymentStatus = com.autowash.enums.PaymentStatus.PAID
        GROUP BY 
            promo.id,  
            promo.voucherCode,
            promo.campaignName,
            promo.discountAmount,
            promo.discountPercent,
            promo.maxDiscountAmount
        ORDER BY COUNT(pay) DESC        
    """)
    List<TopUsedVoucherResponse> findTopUsedVoucher();
}
