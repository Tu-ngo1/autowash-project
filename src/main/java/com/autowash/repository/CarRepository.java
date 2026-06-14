package com.autowash.repository;

import com.autowash.entity.Car;
import com.autowash.entity.User;
import com.autowash.enums.VehicleSize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findByUser(User user);

    List<Car> findByUserId(Long userId);

    Optional<Car> findByIdAndUserId(Long id, Long userId);

    boolean existsByLicensePlate(String licensePlate);

    List<Car> findByVehicleModelVehicleSize(VehicleSize vehicleSize);
}
