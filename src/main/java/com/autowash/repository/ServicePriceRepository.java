package com.autowash.repository;

import com.autowash.entity.ServicePrice;
import com.autowash.enums.VehicleSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicePriceRepository extends JpaRepository<ServicePrice, Long> {

    List<ServicePrice> findByServiceIdAndActiveTrue(Long serviceId);

    Optional<ServicePrice> findByServiceIdAndVehicleSizeAndActiveTrue(
            Long serviceId,
            VehicleSize vehicleSize
    );

    boolean existsByServiceIdAndVehicleSize(Long serviceId, VehicleSize vehicleSize);
}
