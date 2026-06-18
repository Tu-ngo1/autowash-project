package com.autowash.dto.request;

import com.autowash.enums.Role;
import lombok.Data;

@Data
public class RegisterRequest {

    private String fullName;

    private String email;

    private String phone;

    private String username;

    private String password;

    private String otp;

    private String licensePlate;

    private Role role;
}
