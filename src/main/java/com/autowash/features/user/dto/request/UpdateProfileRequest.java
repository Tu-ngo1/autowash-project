package com.autowash.features.user.dto.request;

import com.autowash.features.user.entity.User;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String fullName;
}
