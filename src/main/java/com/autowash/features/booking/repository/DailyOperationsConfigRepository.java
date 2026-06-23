package com.autowash.features.booking.repository;

import com.autowash.features.booking.entity.DailyOperationsConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyOperationsConfigRepository extends JpaRepository<DailyOperationsConfig, Long> {
    Optional<DailyOperationsConfig> findByConfigDate(LocalDate configDate);
}
