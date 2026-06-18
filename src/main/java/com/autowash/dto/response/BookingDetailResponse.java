package com.autowash.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingDetailResponse {

    private Long id;
    private Long serviceId;
    private String serviceName;
    private Integer actualPrice;
    private Integer actualDurationMinutes;
}