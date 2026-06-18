package com.autowash.controller;

import com.autowash.entity.VehicleModel;
import com.autowash.service.VehicleModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/vehicle-models")
@RequiredArgsConstructor
public class VehicleModelController {

    private final VehicleModelService vehicleModelService;

    @GetMapping
    public List<VehicleModel> getAllVehicleModels() {
        return vehicleModelService.getAllVehicleModels();
    }
}
