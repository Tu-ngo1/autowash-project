package com.autowash.controller;

import com.autowash.dto.response.BookingResponse;
import com.autowash.dto.response.BookingStatusResponse;
import com.autowash.dto.response.TopUsedVoucherResponse;
import com.autowash.enums.BookingStatus;
import com.autowash.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AnalyticsService analyticsService;

    @GetMapping("/analytics/bookings-by-status")
    public List<BookingStatusResponse> getBookingsByStatusCount() {
        return analyticsService.countBookingByStatus();
    }

    @GetMapping("/analytics/top-used-vouchers")
    public List<TopUsedVoucherResponse> getTopUsedVouchers() {
        return analyticsService.getTopVoucher();
    }

    @GetMapping("/bookings")
    public List<BookingResponse> getBookings(@RequestParam(required = false) BookingStatus status) {
        return analyticsService.getBookingListByStatus(status);
    }
}
