package com.autowash.dto.request;

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
