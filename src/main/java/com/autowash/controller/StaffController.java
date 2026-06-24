package com.autowash.controller;

import com.autowash.dto.response.BookingResponse;
import com.autowash.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final BookingService bookingService;

    @PostMapping("/bookings/check-in")
    public BookingResponse checkInByQr(@RequestParam String qrContent) {
        return bookingService.checkInByQr(qrContent);
    }
}