package com.autowash.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateServiceRequest {

    private String name;

    private String description;

    private Boolean active;
}