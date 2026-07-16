package com.autowash.features.washservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class CreateServiceRequest {

    @NotBlank(message = "Tên dịch vụ không được để trống")
    private String name;

    private String description;

    private Boolean isMainService;

    private List<ServicePriceConfig> servicePrices;
}
