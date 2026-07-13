package com.autowash.features.user.dto.response;

import com.autowash.features.car.dto.response.CarResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSearchResponse {
    private String fullName;
    private String phone;
    private String tierLevel;
    private Integer rewardPoints;
    private List<CarResponse> registeredVehicles;
}
