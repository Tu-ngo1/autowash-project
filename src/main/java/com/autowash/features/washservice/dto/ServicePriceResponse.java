package com.autowash.features.washservice.dto;

import com.autowash.features.washservice.service.WashService;

import com.autowash.features.car.entity.Car;

import com.autowash.features.car.enums.VehicleSize;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServicePriceResponse {

    private Long id;
    private Long serviceId;
    private String serviceName;
    private VehicleSize vehicleSize;
    private Integer price;
    private Integer durationMinutes;
    private Boolean active;
}


