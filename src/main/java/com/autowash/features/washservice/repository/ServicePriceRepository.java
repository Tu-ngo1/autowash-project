package com.autowash.features.washservice.repository;

import com.autowash.features.washservice.service.WashService;

import com.autowash.features.car.entity.Car;

import com.autowash.features.washservice.entity.ServicePrice;
import com.autowash.features.car.enums.VehicleSize;
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


