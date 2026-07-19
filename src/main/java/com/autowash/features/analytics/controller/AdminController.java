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
import com.autowash.features.promotion.service.VoucherService;
import com.autowash.features.promotion.dto.request.CreateVoucherRequest;
import com.autowash.features.booking.enums.CancelRequestStatus;
import com.autowash.features.booking.dto.response.AdminBookingListResponse;
import com.autowash.features.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AnalyticsService analyticsService;
    private final TierConfigService tierConfigService;
    private final VoucherService voucherService;
    private final BookingService bookingService;

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
    public AdminBookingListResponse getBookings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) CancelRequestStatus cancelRequestStatus,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return bookingService.getAdminBookingsWithFilters(page, limit, status, cancelRequestStatus, search, startDate, endDate);
    }

    @GetMapping("/bookings/{id}")
    public BookingResponse getBookingDetail(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    @PutMapping("/bookings/{id}/status")
    public BookingResponse updateBookingStatusByAdmin(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        BookingStatus status = BookingStatus.valueOf(body.get("status").toUpperCase());
        return bookingService.updateBookingStatusByAdmin(id, status);
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<?> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBookingByAdmin(id);
        return ResponseEntity.ok(Map.of("message", "Đã hủy đơn đặt lịch thành công và hoàn tiền 100% vào ví"));
    }

    @PostMapping("/bookings/{id}/cancel-request/approve")
    public BookingResponse approveCancelRequest(@PathVariable Long id) {
        return bookingService.approveCancelRequest(id);
    }

    @PostMapping("/bookings/{id}/cancel-request/reject")
    public BookingResponse rejectCancelRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String adminNote = body.get("adminNote");
        return bookingService.rejectCancelRequest(id, adminNote);
    }

    @GetMapping("/tiers")
    public List<TierConfigResponse> getTier() {
        return tierConfigService.getTierConfigResponseList();
    }

    @PutMapping("/tiers/{id}")
    public TierConfigResponse updateTierConfig(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return tierConfigService.updateTierConfig(id, body);
    }

    @PostMapping("/vouchers")
    public VoucherResponse createVoucher(@RequestBody CreateVoucherRequest request) {
        return voucherService.createVoucher(request);
    }

    @PutMapping("/vouchers/{id}")
    public VoucherResponse updateVoucher(@PathVariable Long id, @RequestBody CreateVoucherRequest request) {
        return voucherService.updateVoucher(id, request);
    }

    @DeleteMapping("/vouchers/{id}")
    public ResponseEntity<?> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.ok(Map.of("message", "Xóa voucher thành công"));
    }

    @PatchMapping("/vouchers/{id}/status")
    public VoucherResponse updateVoucherStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean isActive = body.get("isActive");
        if (isActive == null) {
            isActive = body.get("active");
        }
        return voucherService.updateStatus(id, isActive != null ? isActive : false);
    }
}


