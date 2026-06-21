package com.autowash.features.booking.dto.response;

import com.autowash.features.booking.enums.PaymentStatus;
import com.autowash.features.booking.enums.PaymentMethod;
import com.autowash.features.booking.entity.Booking;

import com.autowash.features.booking.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private String bookingCode;
    private String customerName;
    private String phone;
    private Long vehicleId;
    private String vehicleLicensePlate;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime expectedEndTime;
    private BookingStatus status;
    private List<String> services;
    private String paymentMethod;
    private String paymentStatus;
    private Integer totalPrice;
    private Integer bayNumber;
    private Boolean late;
    private String customerNote;
    private List<BookingDetailResponse> details;
}


