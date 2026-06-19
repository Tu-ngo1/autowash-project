package com.autowash.features.promotion.repository;

import com.autowash.features.promotion.entity.Promotion;

import com.autowash.features.promotion.entity.CustomerVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, Long> {
    List<CustomerVoucher> findByUserId(Long userId);
}


