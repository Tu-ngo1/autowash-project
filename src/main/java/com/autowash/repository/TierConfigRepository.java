package com.autowash.repository;

import com.autowash.dto.response.TierConfigResponse;
import com.autowash.entity.TierConfig;
import com.autowash.enums.TierLevel;
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

