package com.autowash.repository;

import com.autowash.entity.Payment;
import com.autowash.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPaymentStatus(PaymentStatus paymentStatus);
}
