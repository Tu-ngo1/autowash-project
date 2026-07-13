package com.autowash.features.booking.dto.request;

import com.autowash.features.booking.enums.PaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WalkInBookingRequest {
    @NotBlank(message = "Tên khách hàng không được để trống")
    private String customerName;

    private String customerPhone; // Có thể để trống đối với khách vãng lai vãng lai

    @NotBlank(message = "Biển số xe không được để trống")
    private String licensePlate;

    @NotNull(message = "ID mẫu xe không được để trống")
    private Long vehicleModelId;

    @NotNull(message = "Thời gian đặt lịch không được để trống")
    private LocalDateTime scheduledStartTime;

    @NotEmpty(message = "Phải chọn ít nhất 1 dịch vụ")
    private List<Long> serviceIds;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod; // CASH hoặc BANK_TRANSFER

    private String customerNote;
}
