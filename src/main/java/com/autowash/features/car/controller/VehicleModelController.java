package com.autowash.features.car.controller;

import com.autowash.features.washservice.entity.Service;
import com.autowash.features.car.entity.Car;

import com.autowash.features.car.entity.VehicleModel;
import com.autowash.features.car.service.VehicleModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/customer/vehicle-models", "/api/staff/vehicle-models", "/api/admin/vehicle-models"})
@RequiredArgsConstructor
public class VehicleModelController {

    private final VehicleModelService vehicleModelService;

    @GetMapping
    public List<VehicleModel> getAllVehicleModels() {
        return vehicleModelService.getAllVehicleModels();
    }
}


