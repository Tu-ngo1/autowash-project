package com.autowash.controller;

import com.autowash.dto.request.UpdateBookingStatusRequest;
import com.autowash.dto.response.BookingResponse;
import com.autowash.service.BookingService;
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