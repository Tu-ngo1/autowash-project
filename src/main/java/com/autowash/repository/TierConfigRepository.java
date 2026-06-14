package com.autowash.repository;

import com.autowash.entity.TierConfig;
import com.autowash.enums.TierLevel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TierConfigRepository extends JpaRepository<TierConfig, TierLevel> {
}
