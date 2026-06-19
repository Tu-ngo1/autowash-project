package com.autowash.features.user.dto;

import com.autowash.features.user.entity.User;

import com.autowash.features.user.enums.Role;
import com.autowash.features.user.enums.TierLevel;
import com.autowash.features.user.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private UserStatus status;
    private TierLevel tierLevel;
    private Integer rewardPoints;
    private Integer tierPoints;
    private Integer carCount;
    private Integer bookingCount;
    private Role role;
}


