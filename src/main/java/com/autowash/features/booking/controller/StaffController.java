package com.autowash.features.booking.controller;

import com.autowash.features.booking.dto.request.CheckInQrRequest;
import com.autowash.features.booking.dto.request.BookingAssignRequest;
import com.autowash.features.booking.dto.request.UpdateBookingStatusRequest;
import com.autowash.features.booking.dto.response.BookingResponse;
import com.autowash.features.booking.dto.response.WashBayResponse;
import com.autowash.features.booking.service.BookingService;
import com.autowash.features.booking.dto.request.WalkInBookingRequest;
import com.autowash.features.booking.dto.response.BookingDataResponse;
import com.autowash.features.car.enums.VehicleSize;
import com.autowash.features.user.dto.response.CustomerSearchResponse;
import com.autowash.features.user.service.UserService;
import com.autowash.features.washservice.service.WashService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final BookingService bookingService;
    private final UserService userService;
    private final WashService washService;

    @GetMapping({"/dashboard/pending", "/pending"})
    public List<BookingResponse> getPendingAppointments() {
        return bookingService.getPendingBookingsForToday();
    }

    @PostMapping({"/dashboard/pending/{id}/confirm", "/pending/confirm/{id}"})
    public BookingResponse confirmPendingAppointment(@PathVariable Long id) {
        return bookingService.confirmPendingBooking(id);
    }

    @GetMapping("/queue")
    public List<BookingResponse> getQueueBookings() {
        return bookingService.getQueueBookingsForToday();
    }

    @GetMapping("/bays")
    public List<WashBayResponse> getWashingBays() {
        return bookingService.getWashingBaysStatus();
    }

    @PostMapping("/bays/{bayId}/assign")
    public BookingResponse assignBay(
            @PathVariable Integer bayId,
            @Valid @RequestBody BookingAssignRequest request
    ) {
        return bookingService.assignBookingToBay(request.getBookingId(), bayId);
    }

    @PostMapping("/bays/{bayId}/complete")
    public BookingResponse completeBay(@PathVariable Integer bayId) {
        return bookingService.completeWashingInBay(bayId);
    }

    @PostMapping("/bays/{bayId}/start")
    public BookingResponse startWashingInBay(@PathVariable Integer bayId) {
        return bookingService.startWashingInBay(bayId);
    }

    @PostMapping(value = "/bookings/check-in", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingResponse checkInByQr(@Valid @RequestBody CheckInQrRequest request) {
        return bookingService.checkInByQr(request.getQrContent());
    }

    @PostMapping(value = "/bookings/check-in", params = "qrContent")
    public BookingResponse checkInByQrParam(@RequestParam String qrContent) {
        return bookingService.checkInByQr(qrContent);
    }

    @PutMapping("/bookings/{id}/status")
    public BookingResponse updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request
    ) {
        return bookingService.updateBookingStatus(id, request);
    }

    @GetMapping("/customers/search")
    public CustomerSearchResponse searchCustomer(@RequestParam String query) {
        return userService.searchCustomer(query);
    }

    @GetMapping({"/dashboard/washed", "/washed"})
    public List<BookingResponse> getWashedBookings() {
        return bookingService.getWashedBookingsForToday();
    }

    @PostMapping("/bookings/{id}/checkout")
    public BookingResponse checkoutBooking(
            @PathVariable Long id,
            @RequestParam(required = false) String paymentMethod
    ) {
        return bookingService.checkoutAndHandoverBooking(id, paymentMethod);
    }

    @GetMapping("/bookings/walk-in/data")
    public BookingDataResponse getWalkInBookingData(@RequestParam(required = false) VehicleSize carSize) {
        LocalDate today = LocalDate.now();
        var services = washService.getServicesByVehicleSize(carSize);
        var slots = bookingService.getAvailableSlots(today, 90);
        var businessHours = bookingService.getBusinessHoursForDate(today);
        return BookingDataResponse.builder()
                .services(services)
                .timeSlots(slots)
                .businessHours(businessHours)
                .build();
    }

    @PostMapping("/bookings/walk-in")
    public BookingResponse createWalkInBooking(@Valid @RequestBody WalkInBookingRequest request) {
        return bookingService.createWalkInBooking(request);
    }

    @PostMapping("/bookings/{id}/cancel-request")
    public BookingResponse requestCancelBooking(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String reason = body.get("reason");
        return bookingService.createCancelRequestByStaff(id, reason);
    }

    @PostMapping("/bookings/{id}/add-services")
    public BookingResponse addServicesToBooking(
            @PathVariable Long id,
            @Valid @RequestBody com.autowash.features.booking.dto.request.AddServicesRequest request
    ) {
        return bookingService.addServicesToBooking(id, request);
    }
}
