package com.autowash.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvailableServiceResponse {
    private Long serviceId;
    private String serviceName;
    private String description;
    private Long servicePriceId;
    private Integer price;
    private Integer durationMinutes;
    private String vehicleSize;
}
