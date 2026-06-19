package com.autowash.features.car.service;


import com.autowash.features.user.service.UserService;

import com.autowash.features.car.dto.CreateCarRequest;
import com.autowash.features.car.dto.UpdateCarRequest;
import com.autowash.features.car.dto.CarResponse;
import com.autowash.features.car.entity.Car;
import com.autowash.features.user.entity.User;
import com.autowash.features.car.entity.VehicleModel;
import com.autowash.features.car.enums.CarStatus;
import com.autowash.features.car.repository.CarRepository;
import com.autowash.features.car.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final UserService userService;
    private final VehicleModelRepository vehicleModelRepository;

    public List<CarResponse> getMyCars() {
        User currentUser = userService.getCurrentUserEntity();

        return carRepository.findByUserIdAndStatus(currentUser.getId(), CarStatus.ACTIVE)
                .stream()
                .map(CarResponse::fromCar)
                .toList();
    }

    @Transactional
    public CarResponse createCar(CreateCarRequest request) {
        User currentUser = userService.getCurrentUserEntity();

        if (request.getLicensePlate() == null || request.getLicensePlate().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "License plate is required"
            );
        }

        if (request.getVehicleSize() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vehicle size is required"
            );
        }
        if (request.getVehicleModelId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vehicle Model is required"
            );
        }

        if (carRepository.existsByLicensePlateAndStatus(request.getLicensePlate(), CarStatus.ACTIVE)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "License plate already exists"
            );
        }

        VehicleModel vehicleModel = vehicleModelRepository
                .findById(request.getVehicleModelId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vehicle model not found"
                ));

        Car car = Car.builder()
                .user(currentUser)
                .licensePlate(request.getLicensePlate())
                .vehicleModel(vehicleModel)
                .status(CarStatus.ACTIVE)
                .build();

        Car savedCar = carRepository.save(car);

        return CarResponse.fromCar(savedCar);
    }

    @Transactional
    public CarResponse updateCar(Long carId, UpdateCarRequest request) {
        User currentUser = userService.getCurrentUserEntity();

        Car car = carRepository.findByIdAndUserIdAndStatus(carId, currentUser.getId(), CarStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Car not found"
                ));

        if (request.getLicensePlate() != null && !request.getLicensePlate().isBlank()) {
            if (!request.getLicensePlate().equals(car.getLicensePlate())
                    && carRepository.existsByLicensePlateAndStatus(request.getLicensePlate(), CarStatus.ACTIVE)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "License plate already exists"
                );
            }

            car.setLicensePlate(request.getLicensePlate());
        }

        if (request.getVehicleSize() != null) {
            VehicleModel vehicleModel = vehicleModelRepository
                    .findById(request.getVehicleModelId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Vehicle model not found"
                    ));

            car.setVehicleModel(vehicleModel);
        }

        Car savedCar = carRepository.save(car);

        return CarResponse.fromCar(savedCar);
    }

    @Transactional
    public void deleteCar(Long carId) {
        User currentUser = userService.getCurrentUserEntity();

        Car car = carRepository.findByIdAndUserIdAndStatus(carId, currentUser.getId(), CarStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Car not found"
                ));

        car.setStatus(CarStatus.INACTIVE);
        carRepository.save(car);
    }
}


