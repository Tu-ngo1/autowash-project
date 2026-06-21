package com.autowash.features.user.dto.response;

import com.autowash.features.user.enums.Role;
import com.autowash.features.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String username;
    private String role;
    private String status;
}
