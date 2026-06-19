package com.autowash.features.auth.dto;

import com.autowash.features.user.entity.User;

import com.autowash.features.user.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String role;
    private String dashboardUrl;
}

