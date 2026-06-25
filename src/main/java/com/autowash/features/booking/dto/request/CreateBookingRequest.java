package com.autowash.features.booking.dto.request;

import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.enums.PaymentMethod;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreateBookingRequest {

    @NotNull(message = "Vehicle ID không được để trống")
    private Long vehicleId;

    @NotNull(message = "Thời gian đặt lịch không được để trống")
    @Future(message = "Thời gian đặt lịch phải ở tương lai")
    private LocalDateTime scheduledStartTime;

    @NotEmpty(message = "Phải chọn ít nhất 1 dịch vụ")
    private List<Long> serviceIds;

    private String customerNote;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    private String voucherCode;
}


