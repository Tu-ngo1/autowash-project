package com.autowash.features.car.dto;

import com.autowash.features.car.entity.Car;
import com.autowash.features.car.enums.VehicleSize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CarResponse {

    private Long id;
    private String licensePlate;
    private VehicleSize vehicleSize;
    private Long vehicleModelId;   // Thêm ID dòng xe
    private String brand;          // Thêm Hãng xe (Toyota, Honda...)
    private String modelName;      // Thêm Dòng xe (Vios, Civic...)
    public static CarResponse fromCar(Car car) {
        return CarResponse.builder()
                .id(car.getId())
                .licensePlate(car.getLicensePlate())
                .vehicleSize(car.getVehicleModel().getVehicleSize())
                .vehicleModelId(car.getVehicleModel().getId())
                .brand(car.getVehicleModel().getBrand())
                .modelName(car.getVehicleModel().getModelName())
                .build();
    }



}


