package com.autowash.features.booking.dto.request;

import com.autowash.features.booking.entity.Booking;

import com.autowash.features.booking.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBookingStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private BookingStatus status;
}


