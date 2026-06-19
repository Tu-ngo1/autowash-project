package com.autowash.features.washservice.dto;

import com.autowash.features.washservice.service.WashService;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateServiceRequest {

    @NotBlank(message = "Tên dịch vụ không được để trống")
    private String name;

    private String description;
}

