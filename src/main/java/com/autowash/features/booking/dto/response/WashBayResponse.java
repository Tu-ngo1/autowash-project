package com.autowash.features.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WashBayResponse {
    private Integer id;
    private String name;
    private String type;
    private String status; // "AVAILABLE" or "BUSY"
    private BookingResponse booking;
}
