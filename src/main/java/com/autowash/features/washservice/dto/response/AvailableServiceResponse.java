package com.autowash.features.washservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableServiceResponse {
    private Long serviceId;
    private String serviceName;
    private String description;
    private Long servicePriceId;
    private Integer price;
    private Integer durationMinutes;
    private String vehicleSize;
    private Boolean isMainService;
}
