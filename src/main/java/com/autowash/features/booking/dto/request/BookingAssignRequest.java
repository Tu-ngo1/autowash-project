package com.autowash.features.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingAssignRequest {
    @NotNull(message = "ID booking không được để trống")
    private Long bookingId;
}
