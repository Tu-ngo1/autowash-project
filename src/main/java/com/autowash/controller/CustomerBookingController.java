package com.autowash.controller;

import com.autowash.dto.request.CreateBookingRequest;
import com.autowash.dto.response.BookingResponse;
import com.autowash.dto.response.QrCodeResponse;
import com.autowash.entity.User;
import com.autowash.service.BookingService;
import com.autowash.service.UserService;
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

    // Customer tạo booking mới
    @PostMapping
    public BookingResponse createBooking(@RequestBody CreateBookingRequest request) {
        User currentUser = userService.getCurrentUserEntity();
        return bookingService.createBooking(currentUser.getId(), request);
    }

    // Customer xem lịch sử booking của mình
    @GetMapping("/my")
    public List<BookingResponse> getMyBookings() {
        User currentUser = userService.getCurrentUserEntity();
        return bookingService.getMyBookings(currentUser.getId());
    }

    // Customer xem chi tiết 1 booking
    @GetMapping("/{id}")
    public BookingResponse getBookingById(@PathVariable Long id) {
        User currentUser = userService.getCurrentUserEntity();

        BookingResponse booking = bookingService.getBookingById(id);

        if (booking.getCustomerPhone() == null ||
                !booking.getCustomerPhone().equals(currentUser.getPhone())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền xem thông tin booking này"
            );
        }

        return booking;
    }

    // Customer hủy booking
    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id) {
        User currentUser = userService.getCurrentUserEntity();
        bookingService.cancelBooking(currentUser.getId(), id);
        return "Hủy lịch thành công";
    }

    // Customer lấy QR của booking
    @GetMapping("/{id}/qr")
    public QrCodeResponse getQrCode(@PathVariable Long id) {
        User currentUser = userService.getCurrentUserEntity();
        return bookingService.getQrCode(currentUser.getId(), id);
    }
}