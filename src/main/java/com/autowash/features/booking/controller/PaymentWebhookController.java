package com.autowash.features.booking.controller;

import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.entity.Payment;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.booking.enums.PaymentStatus;
import com.autowash.features.booking.repository.BookingRepository;
import com.autowash.features.booking.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.payos.PayOS;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PayOS payOS;

    @PostMapping("/payos-webhook")
    public ResponseEntity<?> receiveWebhook(@RequestBody Webhook webhookBody) {
        log.info("Received PayOS Webhook: {}", webhookBody);
        try {
            // 1. Verify webhook signature and extract data
            WebhookData data = payOS.webhooks().verify(webhookBody);
            log.info("PayOS Webhook verified successfully. Data: {}", data);

            // 2. PayOS sends orderCode as a long. We set it as booking ID.
            long bookingId = data.getOrderCode();

            // 3. Find the booking
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

            // 4. Update status if payment is successful
            if ("00".equals(data.getCode())) {
                if (booking.getPayment() != null) {
                    Payment payment = booking.getPayment();
                    payment.setPaymentStatus(PaymentStatus.PAID);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }

                booking.setStatus(BookingStatus.CONFIRM);
                bookingRepository.save(booking);
                log.info("Booking ID {} status updated to CONFIRM", bookingId);
            }

            return ResponseEntity.ok(Map.of("message", "Webhook processed successfully"));

        } catch (Exception e) {
            log.error("Error processing PayOS Webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid signature or processing failure"));
        }
    }
}
