package com.autowash.features.user.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateUserPointsRequest {
    @Min(value = 0, message = "Điểm thưởng không được nhỏ hơn 0")
    private Integer rewardPoints;
    
    @Min(value = 0, message = "Điểm hạng không được nhỏ hơn 0")
    private Integer tierPoints;

    private Integer rankPointsDelta;
    private Integer redeemPointsDelta;
}
