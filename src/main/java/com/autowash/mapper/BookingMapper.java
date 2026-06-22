package com.autowash.mapper;

import com.autowash.dto.response.BookingDetailResponse;
import com.autowash.dto.response.BookingResponse;
import com.autowash.entity.Booking;
import com.autowash.entity.BookingDetail;
import com.autowash.entity.CustomerProfile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        List<BookingDetailResponse> details = booking.getBookingDetails() == null
                ? List.of()
                : booking.getBookingDetails()
                .stream()
                .map(this::toDetailResponse)
                .toList();

        List<String> services = details.stream()
                .map(BookingDetailResponse::getServiceName)
                .toList();

        String paymentMethod = null;
        String paymentStatus = null;

        if (booking.getPayment() != null) {
            paymentMethod = booking.getPayment().getPaymentMethod() != null
                    ? booking.getPayment().getPaymentMethod().name()
                    : null;

            paymentStatus = booking.getPayment().getPaymentStatus() != null
                    ? booking.getPayment().getPaymentStatus().name()
                    : null;
        }

        Integer totalPrice = booking.getTotalPrice();

        if (totalPrice == null) {
            totalPrice = details.stream()
                    .mapToInt(d -> d.getActualPrice() == null ? 0 : d.getActualPrice())
                    .sum();
        }

        String tierLevel = null;

        if (booking.getUser() != null && booking.getUser().getCustomerProfile() != null) {
            CustomerProfile profile = booking.getUser().getCustomerProfile();

            if (profile.getTierConfig() != null
                    && profile.getTierConfig().getTierLevel() != null) {
                tierLevel = profile.getTierConfig().getTierLevel().name();
            }
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())

                .customerName(booking.getUser() != null ? booking.getUser().getFullName() : null)
                .customerPhone(booking.getUser() != null ? booking.getUser().getPhone() : null)
                .customerEmail(booking.getUser() != null ? booking.getUser().getEmail() : null)

                .vehicleId(booking.getVehicle() != null ? booking.getVehicle().getId() : null)
                .vehicleLicensePlate(booking.getVehicle() != null ? booking.getVehicle().getLicensePlate() : null)

                .scheduledStartTime(booking.getScheduledStartTime())
                .expectedEndTime(booking.getExpectedEndTime())

                .status(booking.getStatus())
                .services(services)

                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus)

                .totalPrice(totalPrice)
                .bayNumber(booking.getBayNumber())
                .late(booking.getLate())
                .customerNote(booking.getCustomerNote())

                .tierLevel(tierLevel)
                .discount(0)

                .details(details)
                .build();
    }

    private BookingDetailResponse toDetailResponse(BookingDetail detail) {
        if (detail == null) {
            return null;
        }

        Long serviceId = null;
        String serviceName = null;

        if (detail.getServicePrice() != null
                && detail.getServicePrice().getService() != null) {
            serviceId = detail.getServicePrice().getService().getId();
            serviceName = detail.getServicePrice().getService().getName();
        }

        return BookingDetailResponse.builder()
                .id(detail.getId())
                .serviceId(serviceId)
                .serviceName(serviceName)
                .actualPrice(detail.getActualPrice())
                .actualDurationMinutes(detail.getActualDurationMinutes())
                .build();
    }
}