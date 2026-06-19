package com.autowash.features.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRatioResponse {
    private String name;
    private String label;
    private Long value;
    private Long count;
}

