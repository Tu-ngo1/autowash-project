package com.autowash.dto.response;

import com.autowash.enums.VehicleSize;
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
