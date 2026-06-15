package com.autowash.mapper;

import com.autowash.dto.response.BookingResponse;
import com.autowash.entity.Booking;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        if (booking == null) return null;

        List<String> serviceList = booking.getBookingDetails().stream()
                .map(bookingDetail -> bookingDetail.getServicePrice() != null
                        && bookingDetail.getServicePrice().getService() != null ?
                        bookingDetail.getServicePrice().getService().getName() : "")
                .toList();

        String payMethod = null;
        String payStatus = null;
        Integer totalPrice = 0;

        if (booking.getPayment() != null) {
            payMethod = booking.getPayment().getPaymentMethod() != null ?
                    booking.getPayment().getPaymentMethod().name() : null;
            payStatus = booking.getPayment().getPaymentStatus() != null ?
                    booking.getPayment().getPaymentStatus().name() : null;
            totalPrice = booking.getPayment().getFinalPrice();
        } else {
            totalPrice = booking.getBookingDetails().stream()
                    .mapToInt(bookingDetail -> bookingDetail.getActualPrice() != null ?
                            bookingDetail.getActualPrice() : 0)
                    .sum();
        }

        return new BookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getUser() != null ? booking.getUser().getFullName() : null,
                booking.getUser() != null ? booking.getUser().getPhone() : null,
                booking.getVehicle() != null ? booking.getVehicle().getLicensePlate() : null,
                booking.getScheduledStartTime(),
                booking.getStatus(),
                serviceList,
                payMethod,
                payStatus,
                totalPrice
        );
    }
}
