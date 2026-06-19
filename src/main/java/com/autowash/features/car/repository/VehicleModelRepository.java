package com.autowash.features.car.repository;

import com.autowash.features.car.entity.Car;

import com.autowash.features.car.entity.VehicleModel;
import com.autowash.features.car.enums.VehicleSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

    List<VehicleModel> findByVehicleSize(VehicleSize vehicleSize);
}


