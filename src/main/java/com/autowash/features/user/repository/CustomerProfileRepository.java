package com.autowash.features.user.repository;

import com.autowash.features.user.entity.CustomerProfile;
import com.autowash.features.user.entity.User;
import com.autowash.features.user.entity.TierConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByUser(User user);

    Optional<CustomerProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    long countByTierConfig(TierConfig tierConfig);
}


