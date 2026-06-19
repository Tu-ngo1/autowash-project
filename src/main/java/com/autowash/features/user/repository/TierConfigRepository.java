package com.autowash.features.user.repository;

import com.autowash.features.user.entity.CustomerProfile;
import com.autowash.features.user.entity.User;

import com.autowash.features.user.dto.TierConfigResponse;
import com.autowash.features.user.entity.TierConfig;
import com.autowash.features.user.enums.TierLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TierConfigRepository extends JpaRepository<TierConfig, TierLevel> {

    @Query("""
        SELECT new com.autowash.dto.response.TierConfigResponse(
            tc.tierLevel,
            tc.pointsToUpgrade,
            tc.autoDiscountPercent,
            COUNT(cp)
        )
        FROM TierConfig tc
        LEFT JOIN CustomerProfile cp ON cp.tierConfig = tc
        GROUP BY tc.tierLevel, tc.pointsToUpgrade, tc.autoDiscountPercent
    """)
    List<TierConfigResponse> findTiersWithCustomerCount();
}


