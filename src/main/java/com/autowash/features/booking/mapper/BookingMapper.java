package com.autowash.features.booking.mapper;

import com.autowash.features.booking.enums.PaymentStatus;
import com.autowash.features.booking.enums.PaymentMethod;

import com.autowash.features.booking.dto.response.BookingDetailResponse;
import com.autowash.features.booking.dto.response.BookingResponse;
import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.entity.BookingDetail;
import com.autowash.features.user.entity.CustomerProfile;
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

        Integer totalPrice = booking.getTotalPrice();

        if (totalPrice == null) {
            totalPrice = details.stream()
                    .mapToInt(detail -> detail.getActualPrice() == null ? 0 : detail.getActualPrice())
                    .sum();
        }

        String paymentMethod = null;
        String paymentStatus = null;
        Integer finalPrice = totalPrice;
        Integer discount = 0;
        Integer actualPaidAmount = null;

        if (booking.getPayment() != null) {
            paymentMethod = booking.getPayment().getPaymentMethod() != null
                    ? booking.getPayment().getPaymentMethod().name()
                    : null;

            paymentStatus = booking.getPayment().getPaymentStatus() != null
                    ? booking.getPayment().getPaymentStatus().name()
                    : null;

            if (booking.getPayment().getFinalPrice() != null) {
                finalPrice = booking.getPayment().getFinalPrice();
            }
            if (booking.getPayment().getDiscountAmount() != null) {
                discount = booking.getPayment().getDiscountAmount();
            }
            actualPaidAmount = booking.getPayment().getActualPaidAmount();
            if (actualPaidAmount == null && PaymentStatus.PAID.name().equals(paymentStatus)) {
                actualPaidAmount = finalPrice;
            }
        }

        String tierLevel = null;
        if (booking.getUser() != null && booking.getUser().getCustomerProfile() != null) {
            CustomerProfile profile = booking.getUser().getCustomerProfile();
            if (profile.getTierConfig() != null && profile.getTierConfig().getTierLevel() != null) {
                tierLevel = profile.getTierConfig().getTierLevel().name();
            }
        }

        String vehicleSize = null;
        if (booking.getVehicle() != null && booking.getVehicle().getVehicleModel() != null && booking.getVehicle().getVehicleModel().getVehicleSize() != null) {
            vehicleSize = booking.getVehicle().getVehicleModel().getVehicleSize().name();
        }

        Integer reviewRating = null;
        String reviewComment = null;
        java.time.LocalDateTime reviewCreatedAt = null;

        if (booking.getReview() != null) {
            reviewRating = booking.getReview().getRating();
            reviewComment = booking.getReview().getComment();
            reviewCreatedAt = booking.getReview().getCreatedAt();
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .customerName(booking.getUser() != null ? booking.getUser().getFullName() : null)
                .customerPhone(booking.getUser() != null ? booking.getUser().getPhone() : null)
                .customerEmail(booking.getUser() != null ? booking.getUser().getEmail() : null)
                .vehicleId(booking.getVehicle() != null ? booking.getVehicle().getId() : null)
                .vehicleLicensePlate(booking.getVehicle() != null ? booking.getVehicle().getLicensePlate() : null)
                .vehicleSize(vehicleSize)
                .scheduledStartTime(booking.getScheduledStartTime())
                .expectedEndTime(booking.getExpectedEndTime())
                .status(booking.getStatus())
                .services(services)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus)
                .totalPrice(totalPrice)
                .finalPrice(finalPrice)
                .actualPaidAmount(actualPaidAmount)
                .bayNumber(booking.getBayNumber())
                .late(booking.getLate())
                .qrUsed(booking.getQrUsed())
                .arrivedAt(booking.getArrivedAt())
                .washStartedAt(booking.getWashStartedAt())
                .completedAt(booking.getCompletedAt())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .customerNote(booking.getCustomerNote())
                .tierLevel(tierLevel)
                .discount(discount)
                .details(details)
                .cancelRequestStatus(booking.getCancelRequestStatus() != null ? booking.getCancelRequestStatus().name() : null)
                .cancelRequestReason(booking.getCancelRequestReason())
                .cancelRequestedByName(booking.getCancelRequestedBy() != null ? booking.getCancelRequestedBy().getFullName() : null)
                .cancelRequestedAt(booking.getCancelRequestedAt())
                .cancelRequestAdminNote(booking.getCancelRequestAdminNote())
                .reviewRating(reviewRating)
                .reviewComment(reviewComment)
                .reviewCreatedAt(reviewCreatedAt)
                .build();
    }

    private BookingDetailResponse toDetailResponse(BookingDetail detail) {
        if (detail == null) {
            return null;
        }

        String serviceName = null;
        Long serviceId = null;

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


