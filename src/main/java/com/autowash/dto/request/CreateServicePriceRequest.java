package com.autowash.dto.request;

import com.autowash.enums.VehicleSize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateServicePriceRequest {

    @NotNull(message = "Service ID không được để trống")
    private Long serviceId;

    @NotNull(message = "Kích cỡ xe không được để trống")
    private VehicleSize vehicleSize;

    @NotNull(message = "Giá không được để trống")
    @Min(value = 0, message = "Giá không được âm")
    private Integer price;

    @NotNull(message = "Thời lượng không được để trống")
    @Min(value = 1, message = "Thời lượng phải lớn hơn 0")
    private Integer durationMinutes;
}
