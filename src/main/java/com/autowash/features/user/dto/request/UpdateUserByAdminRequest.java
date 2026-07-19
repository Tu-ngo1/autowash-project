package com.autowash.features.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserByAdminRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    private String email;
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;
}
