package com.autowash.features.car.dto.request;


import lombok.Data;

@Data
public class UpdateCarRequest {

    private String licensePlate;

    private Long vehicleModelId;
}


