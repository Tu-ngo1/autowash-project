package com.autowash.dto.response;

import com.autowash.enums.Role;
import com.autowash.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String fullName;
    private String phone;
    private Role role;
    private UserStatus status;
}