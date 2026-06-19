package com.autowash.features.car.repository;

import com.autowash.features.car.entity.Car;
import com.autowash.features.user.entity.User;
import com.autowash.features.car.enums.CarStatus;
import com.autowash.features.car.enums.VehicleSize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findByUser(User user);

    List<Car> findByUserId(Long userId);

    List<Car> findByUserIdAndStatus(Long userId, CarStatus status);

    Optional<Car> findByIdAndUserId(Long id, Long userId);

    Optional<Car> findByIdAndUserIdAndStatus(Long id, Long userId, CarStatus status);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndStatus(String licensePlate, CarStatus status);

    List<Car> findByVehicleModelVehicleSize(VehicleSize vehicleSize);

    int countByUserId(Long userId);

    int countByUserIdAndStatus(Long userId, CarStatus status);
}


