package com.autowash.features.washservice.dto;

import com.autowash.features.washservice.service.WashService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateServiceRequest {

    private String name;

    private String description;

    private Boolean active;
}
