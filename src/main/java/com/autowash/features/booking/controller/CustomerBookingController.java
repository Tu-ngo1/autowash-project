package com.autowash.features.booking.controller;

import com.autowash.features.washservice.entity.Service;
import com.autowash.features.booking.entity.Booking;

import com.autowash.features.booking.dto.request.CreateBookingRequest;
import com.autowash.features.booking.dto.response.BookingResponse;
import com.autowash.features.booking.dto.response.QrCodeResponse;
import com.autowash.features.user.entity.User;
import com.autowash.features.booking.service.BookingService;
import com.autowash.features.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDate;
import com.autowash.features.booking.dto.response.BookingDataResponse;
import com.autowash.features.car.enums.VehicleSize;
import com.autowash.features.washservice.service.WashService;

@RestController
@RequestMapping("/api/customer/bookings")
@RequiredArgsConstructor
public class CustomerBookingController {

    private final BookingService bookingService;
    private final UserService userService;
    private final WashService washService;

    @GetMapping("/data")
    public BookingDataResponse getBookingData(
            @RequestParam VehicleSize carSize,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        var services = washService.getServicesByVehicleSize(carSize);
        var slots = bookingService.getAvailableSlots(date);
        return BookingDataResponse.builder()
                .services(services)
                .availableSlots(slots)
                .build();
    }

    @PostMapping
    public BookingResponse createBooking(@RequestBody CreateBookingRequest request) {
        User currentUser = userService.getCurrentUserEntity();
        return bookingService.createBooking(currentUser.getId(), request);
    }

    @GetMapping("/my")
    public List<BookingResponse> getMyBookings() {
        User currentUser = userService.getCurrentUserEntity();
        return bookingService.getMyBookings(currentUser.getId());
    }

    @GetMapping("/{id}")
    public BookingResponse getBookingById(@PathVariable Long id) {
        User currentUser = userService.getCurrentUserEntity();
        BookingResponse booking = bookingService.getBookingById(id);
        
        if (!booking.getCustomerPhone().equals(currentUser.getPhone())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền xem thông tin booking này"
            );
        }
        
        return booking;
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id) {
        User currentUser = userService.getCurrentUserEntity();
        bookingService.cancelBooking(currentUser.getId(), id);
        return "Hủy lịch thành công";
    }

    @GetMapping("/{id}/qr")
    public QrCodeResponse getQrCode(@PathVariable Long id) {
        User currentUser = userService.getCurrentUserEntity();
        return bookingService.getQrCode(currentUser.getId(), id);
    }
}


