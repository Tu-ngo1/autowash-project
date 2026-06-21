package com.autowash.features.analytics.controller;

import com.autowash.features.washservice.entity.Service;

import com.autowash.features.promotion.entity.Promotion;
import com.autowash.features.booking.entity.Booking;
import com.autowash.features.user.entity.User;

import com.autowash.features.booking.dto.response.BookingResponse;
import com.autowash.features.analytics.dto.response.BookingStatusResponse;
import com.autowash.features.analytics.dto.response.RevenueResponse;
import com.autowash.features.user.dto.response.TierConfigResponse;
import com.autowash.features.promotion.dto.response.TopUsedVoucherResponse;
import com.autowash.features.promotion.dto.response.VoucherResponse;
import com.autowash.features.analytics.dto.response.DashboardAnalyticsResponse;
import com.autowash.features.analytics.enums.AnalyticsPeriod;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.analytics.service.AnalyticsService;
import com.autowash.features.user.service.TierConfigService;
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
    private final TierConfigService tierConfigService;

    @GetMapping("/analytics/dashboard")
    public DashboardAnalyticsResponse getDashboardAnalytics() {
        return analyticsService.getDashboardAnalytics();
    }

    @GetMapping("/analytics/bookings-by-status")
    public List<BookingStatusResponse> getBookingsByStatusCount() {
        return analyticsService.countBookingByStatus();
    }

    @GetMapping("/analytics/top-used-vouchers")
    public List<TopUsedVoucherResponse> getTopUsedVouchers() {
        return analyticsService.getTopVoucher();
    }

    @GetMapping("/analytics/revenue")
    public List<RevenueResponse> getRevenueAnalytics(
            @RequestParam(defaultValue = "DAY") AnalyticsPeriod period
    ) {
        return analyticsService.getRevenueAnalytics(period);
    }

    @GetMapping("/vouchers")
    public List<VoucherResponse> getAllVoucher() {
        return analyticsService.getAllVoucher();
    }

    @GetMapping("/bookings")
    public List<BookingResponse> getBookings(@RequestParam(required = false) BookingStatus status) {
        return analyticsService.getBookingListByStatus(status);
    }

    @GetMapping("/tiers")
    public List<TierConfigResponse> getTier() {
        return tierConfigService.getTierConfigResponseList();
    }
}


