package com.autowash.dto.response;

import com.autowash.enums.TierLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileResponse {

    private Long id;
    private Long userId;
    private TierLevel tierLevel;
    private Integer rewardPoints;
    private Integer tierPoints;
}