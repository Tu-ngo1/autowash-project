package com.autowash.features.booking.controller;

import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.entity.Payment;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.booking.enums.PaymentStatus;
import com.autowash.features.booking.repository.BookingRepository;
import com.autowash.features.booking.repository.PaymentRepository;
import com.autowash.features.user.repository.UserRepository;
import com.autowash.features.wallet.entity.Wallet;
import com.autowash.features.wallet.entity.WalletTransaction;
import com.autowash.features.wallet.enums.WalletTransactionType;
import com.autowash.features.wallet.repository.WalletRepository;
import com.autowash.features.wallet.repository.WalletTransactionRepository;
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
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    @PostMapping("/payos-webhook")
    public ResponseEntity<?> receiveWebhook(@RequestBody Webhook webhookBody) {
        log.info("Received PayOS Webhook: {}", webhookBody);
        try {
            // 1. Verify webhook signature and extract data
            WebhookData data = payOS.webhooks().verify(webhookBody);
            log.info("PayOS Webhook verified successfully. Data: {}", data);

            long orderCode = data.getOrderCode();
            if (orderCode >= 5000000000000000L) {
                // Đây là giao dịch nạp tiền vào ví!
                long userId = (orderCode - 5000000000000000L) / 10000000L;

                // Tìm ví của user hoặc tự động tạo mới
                Wallet wallet = walletRepository.findByUserId(userId)
                        .orElseGet(() -> {
                            com.autowash.features.user.entity.User user = userRepository.findById(userId)
                                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ví và người dùng"));
                            Wallet newWallet = Wallet.builder()
                                    .user(user)
                                    .balance(0)
                                    .build();
                            return walletRepository.save(newWallet);
                        });

                // Kiểm tra trùng lặp giao dịch (Idempotency) để tránh cộng tiền 2 lần
                String txDescription = "Nạp tiền qua PayOS - GD " + orderCode;
                boolean txExists = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId())
                        .stream()
                        .anyMatch(tx -> txDescription.equals(tx.getDescription()));

                if (!txExists && "00".equals(data.getCode())) {
                    int depositAmount = data.getAmount() != null ? data.getAmount().intValue() : 0;
                    // Cộng tiền vào ví
                    wallet.setBalance(wallet.getBalance() + depositAmount);
                    walletRepository.save(wallet);

                    // Ghi giao dịch ví
                    WalletTransaction transaction = WalletTransaction.builder()
                            .wallet(wallet)
                            .amount(depositAmount)
                            .transactionType(WalletTransactionType.DEPOSIT)
                            .description(txDescription)
                            .build();
                    walletTransactionRepository.save(transaction);
                    log.info("Wallet ID {} deposited with {} via PayOS", wallet.getId(), depositAmount);
                }
            } else {
                // Đây là giao dịch đặt lịch (mã gốc bookingId)
                long bookingId = orderCode;

                // Find the booking
                Booking booking = bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

                // Update status if payment is successful
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
            }

            return ResponseEntity.ok(Map.of("message", "Webhook processed successfully"));

        } catch (Exception e) {
            log.error("Error processing PayOS Webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid signature or processing failure"));
        }
    }
}
