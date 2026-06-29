package com.autowash.features.booking.repository;

import com.autowash.features.booking.entity.Booking;

import com.autowash.features.promotion.entity.Promotion;
import com.autowash.features.promotion.entity.CustomerVoucher;

import com.autowash.features.promotion.dto.response.TopUsedVoucherResponse;
import com.autowash.features.booking.entity.Payment;
import com.autowash.features.booking.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPaymentStatus(PaymentStatus paymentStatus);

    Optional<Payment> findByBookingId(Long bookingId);

    @Query("""
        SELECT new com.autowash.features.promotion.dto.response.TopUsedVoucherResponse(
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
        WHERE pay.paymentStatus = com.autowash.features.booking.enums.PaymentStatus.PAID
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


