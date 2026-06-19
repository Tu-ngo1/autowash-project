package com.autowash.features.user.dto;

import com.autowash.features.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileResponse {

    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private Integer rewardPoints;
    private Integer tierPoints;
}
