package com.autowash.repository;

import com.autowash.entity.CustomerProfile;
import com.autowash.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByUser(User user);

    Optional<CustomerProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
