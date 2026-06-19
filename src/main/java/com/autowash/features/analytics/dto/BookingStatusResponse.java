package com.autowash.features.analytics.dto;

import com.autowash.features.booking.entity.Booking;

import com.autowash.features.booking.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusResponse {
    private BookingStatus status;
    private Long total;
}

