package com.autowash.dto.response;

import com.autowash.entity.Car;
import com.autowash.enums.VehicleSize;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CarResponse {

    private Long id;
    private String licensePlate;
    private VehicleSize vehicleSize;

    public static CarResponse fromCar(Car car) {



        return new CarResponse(
                car.getId(),
                car.getLicensePlate(),
                car.getVehicleModel().getVehicleSize()
        );
    }



}
