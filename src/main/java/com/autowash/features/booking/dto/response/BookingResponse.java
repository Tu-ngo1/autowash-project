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
    private String customerPhone;
    private String customerEmail;
    private Long vehicleId;
    private String vehicleLicensePlate;
    private String vehicleSize;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime expectedEndTime;
    private BookingStatus status;
    private List<String> services;
    private String paymentMethod;
    private String paymentStatus;
    private Integer totalPrice;
    private Integer finalPrice;
    private Integer actualPaidAmount;
    private Integer bayNumber;
    private Boolean late;
    private Boolean qrUsed;
    private LocalDateTime arrivedAt;
    private LocalDateTime washStartedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String customerNote;
    private String tierLevel;
    private Integer discount;
    private List<BookingDetailResponse> details;
    private String cancelRequestStatus;
    private String cancelRequestReason;
    private String cancelRequestedByName;
    private LocalDateTime cancelRequestedAt;
    private String cancelRequestAdminNote;
    private String checkoutUrl;
    private Integer reviewRating;
    private String reviewComment;
    private LocalDateTime reviewCreatedAt;
}


