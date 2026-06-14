package com.autowash.dto.response;

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