package com.autowash.features.booking.repository;

import com.autowash.features.booking.entity.DailyOperationsConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyOperationsConfigRepository extends JpaRepository<DailyOperationsConfig, Long> {
    Optional<DailyOperationsConfig> findByConfigDate(LocalDate configDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DailyOperationsConfig d WHERE d.configDate = :configDate")
    Optional<DailyOperationsConfig> findByConfigDateWithLock(@Param("configDate") LocalDate configDate);
}
