package com.autowash.service;

import com.autowash.dto.request.CreateCarRequest;
import com.autowash.dto.request.UpdateCarRequest;
import com.autowash.dto.response.CarResponse;
import com.autowash.entity.Car;
import com.autowash.entity.User;
import com.autowash.entity.VehicleModel;
import com.autowash.repository.CarRepository;
import com.autowash.repository.VehicleModelRepository;
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

        return carRepository.findByUserId(currentUser.getId())
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

        if (carRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "License plate already exists"
            );
        }

        VehicleModel vehicleModel = vehicleModelRepository
                .findByVehicleSize(request.getVehicleSize())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vehicle model not found"
                ));

        Car car = Car.builder()
                .user(currentUser)
                .licensePlate(request.getLicensePlate())
                .vehicleModel(vehicleModel)
                .build();

        Car savedCar = carRepository.save(car);

        return CarResponse.fromCar(savedCar);
    }

    @Transactional
    public CarResponse updateCar(Long carId, UpdateCarRequest request) {
        User currentUser = userService.getCurrentUserEntity();

        Car car = carRepository.findByIdAndUserId(carId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Car not found"
                ));

        if (request.getLicensePlate() != null && !request.getLicensePlate().isBlank()) {
            if (!request.getLicensePlate().equals(car.getLicensePlate())
                    && carRepository.existsByLicensePlate(request.getLicensePlate())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "License plate already exists"
                );
            }

            car.setLicensePlate(request.getLicensePlate());
        }

        if (request.getVehicleSize() != null) {
            VehicleModel vehicleModel = vehicleModelRepository
                    .findByVehicleSize(request.getVehicleSize())
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

        Car car = carRepository.findByIdAndUserId(carId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Car not found"
                ));

        carRepository.delete(car);
    }
}