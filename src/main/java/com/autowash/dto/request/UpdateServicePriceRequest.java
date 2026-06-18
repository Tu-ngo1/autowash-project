package com.autowash.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateServicePriceRequest {

    @Min(value = 0, message = "Giá không được âm")
    private Integer price;

    @Min(value = 1, message = "Thời lượng phải lớn hơn 0")
    private Integer durationMinutes;

    private Boolean active;
}