package com.autowash.features.user.dto.response;

import com.autowash.features.car.dto.response.CarResponse;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AdminUserDetailResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String username;
    private String role;
    private String status;
    private String tier;
    private Integer tierPoints;
    private Integer rewardPoints;
    private Integer walletBalance;
    private List<CarResponse> vehicles;
}
