package com.autowash.features.booking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddServicesRequest {
    @NotEmpty(message = "Danh sách dịch vụ bổ sung không được để trống")
    private List<Long> serviceIds;
}
