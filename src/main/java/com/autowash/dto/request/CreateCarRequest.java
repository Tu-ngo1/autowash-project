package com.autowash.dto.request;

import com.autowash.enums.VehicleSize;
import lombok.Data;

@Data
public class CreateCarRequest {

    private String licensePlate;
    private VehicleSize vehicleSize;
}
