package com.autowash.features.washservice.dto.request;

import com.autowash.features.car.enums.VehicleSize;
import lombok.Data;

@Data
public class ServicePriceConfig {
    private VehicleSize vehicleSize;
    private Integer price;
    private Integer duration; // thời gian rửa (phút)
    private Boolean active;
}
