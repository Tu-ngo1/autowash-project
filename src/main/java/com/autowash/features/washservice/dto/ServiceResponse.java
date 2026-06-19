package com.autowash.features.washservice.dto;

import com.autowash.features.washservice.service.WashService;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServiceResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean active;
}
