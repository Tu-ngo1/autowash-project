package com.autowash.features.booking.dto.response;

import com.autowash.features.washservice.dto.response.AvailableServiceResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDataResponse {
    private List<AvailableServiceResponse> services;
    private List<AvailableSlotResponse> availableSlots;
}
