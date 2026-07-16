package com.autowash.features.washservice.dto.response;

import com.autowash.features.car.enums.VehicleSize;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AdminServiceResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean active;
    private Boolean isMainService;
    private List<PriceDetail> servicePrices;

    @Data
    @Builder
    public static class PriceDetail {
        private Long id;
        private VehicleSize vehicleSize;
        private Integer price;
        private Integer duration;
        private Boolean active;
    }
}
