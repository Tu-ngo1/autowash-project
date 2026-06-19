package com.autowash.features.booking.controller;

import com.autowash.features.washservice.entity.Service;
import com.autowash.features.booking.entity.Booking;

import com.autowash.features.booking.dto.UpdateBookingStatusRequest;
import com.autowash.features.booking.dto.BookingResponse;
import com.autowash.features.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/bookings")
@RequiredArgsConstructor
public class StaffController {

    private final BookingService bookingService;

    @PostMapping("/check-in")
    public BookingResponse checkInByQr(@RequestParam String qrContent) {
        return bookingService.checkInByQr(qrContent);
    }

    @PutMapping("/{id}/status")
    public BookingResponse updateBookingStatus(
            @PathVariable Long id,
            @RequestBody UpdateBookingStatusRequest request
    ) {
        return bookingService.updateBookingStatus(id, request);
    }
}

