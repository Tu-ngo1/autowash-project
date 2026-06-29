package com.autowash.features.promotion.repository;

import com.autowash.features.promotion.entity.CustomerVoucher;
import com.autowash.features.promotion.enums.VoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, Long> {
    List<CustomerVoucher> findByUserId(Long userId);

    Optional<CustomerVoucher> findByUserIdAndVoucherCodeAndStatus(Long userId, String voucherCode, VoucherStatus status);
}



