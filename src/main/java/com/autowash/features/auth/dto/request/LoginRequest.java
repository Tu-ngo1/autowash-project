package com.autowash.features.auth.dto.request;

import lombok.Data;

@Data
public class LoginRequest {

    private String usernameOrPhone;
    private String password;
}

