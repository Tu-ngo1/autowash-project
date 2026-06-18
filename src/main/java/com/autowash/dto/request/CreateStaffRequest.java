package com.autowash.dto.request;

import lombok.Data;

@Data
public class CreateStaffRequest {

    private String fullName;
    private String phone;
    private String email;
    private String username;
    private String password;
}
