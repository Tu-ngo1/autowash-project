package com.autowash.features.car.dto;

import com.autowash.features.car.entity.Car;

import com.autowash.features.car.enums.VehicleSize;
import lombok.Data;

@Data
public class UpdateCarRequest {

    private String licensePlate;
    private VehicleSize vehicleSize;
    private Long vehicleModelId;
}


