package com.autowash.controller;

import com.autowash.dto.request.CreateCarRequest;
import com.autowash.dto.request.UpdateCarRequest;
import com.autowash.dto.response.CarResponse;
import com.autowash.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @GetMapping
    public List<CarResponse> getMyCars() {
        return carService.getMyCars();
    }

    @PostMapping
    public CarResponse createCar(@RequestBody CreateCarRequest request) {
        return carService.createCar(request);
    }

    @PutMapping("/{id}")
    public CarResponse updateCar(
            @PathVariable Long id,
            @RequestBody UpdateCarRequest request
    ) {
        return carService.updateCar(id, request);
    }

    @DeleteMapping("/{id}")
    public String deleteCar(@PathVariable Long id) {
        carService.deleteCar(id);
        return "Car deleted successfully";
    }
}