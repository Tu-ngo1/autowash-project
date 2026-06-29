package com.autowash.features.booking.controller;

import com.autowash.features.booking.dto.request.CheckInQrRequest;
import com.autowash.features.booking.dto.request.UpdateBookingStatusRequest;
import com.autowash.features.booking.dto.response.BookingResponse;
import com.autowash.features.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/bookings")
@RequiredArgsConstructor
public class StaffController {

    private final BookingService bookingService;

    @PostMapping(value = "/check-in", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingResponse checkInByQr(@Valid @RequestBody CheckInQrRequest request) {
        return bookingService.checkInByQr(request.getQrContent());
    }

    @PostMapping(value = "/check-in", params = "qrContent")
    public BookingResponse checkInByQrParam(@RequestParam String qrContent) {
        return bookingService.checkInByQr(qrContent);
    }

    @PutMapping("/{id}/status")
    public BookingResponse updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request
    ) {
        return bookingService.updateBookingStatus(id, request);
    }
}
