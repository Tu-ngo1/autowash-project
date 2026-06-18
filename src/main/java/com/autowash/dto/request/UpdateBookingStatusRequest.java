package com.autowash.dto.request;

import com.autowash.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBookingStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private BookingStatus status;
}
