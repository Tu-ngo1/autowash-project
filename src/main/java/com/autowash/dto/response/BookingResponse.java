package com.autowash.dto.response;

import com.autowash.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private String bookingCode;
    private String customerName;
    private String phone;
    private String vehicleLicensePlate;
    private LocalDateTime scheduledStartTime;
    private BookingStatus status;
    private List<String> services;
    private String paymentMethod;
    private String paymentStatus;
    private Integer totalPrice;
}
