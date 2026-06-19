package com.autowash.features.booking.controller;

import com.autowash.features.washservice.entity.Service;
import com.autowash.features.booking.entity.Booking;

import com.autowash.features.booking.dto.CreateBookingRequest;
import com.autowash.features.booking.dto.BookingResponse;
import com.autowash.features.booking.dto.QrCodeResponse;
import com.autowash.features.user.entity.User;
import com.autowash.features.booking.service.BookingService;
import com.autowash.features.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/customer/bookings")
@RequiredArgsConstructor
public class CustomerBookingController {

    private final BookingService bookingService;
    private final UserService userService;

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
        
        if (!booking.getPhone().equals(currentUser.getPhone())) {
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


