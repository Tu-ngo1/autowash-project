package com.autowash.repository;

import com.autowash.entity.VehicleModel;
import com.autowash.enums.VehicleSize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

    Optional<VehicleModel> findByVehicleSize(VehicleSize vehicleSize);
}
