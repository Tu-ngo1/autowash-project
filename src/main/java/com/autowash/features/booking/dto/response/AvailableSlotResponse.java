package com.autowash.features.booking.dto.response;

import com.autowash.features.booking.entity.Booking;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AvailableSlotResponse {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean available;
}
