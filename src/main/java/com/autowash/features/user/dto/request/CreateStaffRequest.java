package com.autowash.features.user.dto.request;

import com.autowash.features.user.entity.User;

import lombok.Data;

@Data
public class CreateStaffRequest {

    private String fullName;
    private String phone;
    private String email;
    private String username;
    private String password;
}

