package com.autowash.features.wallet.service;

import com.autowash.features.user.entity.User;
import com.autowash.features.user.service.UserService;
import com.autowash.features.wallet.dto.request.DepositRequest;
import com.autowash.features.wallet.dto.response.DepositResponse;
import com.autowash.features.wallet.dto.response.WalletTransactionResponse;
import com.autowash.features.wallet.entity.Wallet;
import com.autowash.features.wallet.entity.WalletTransaction;
import com.autowash.features.wallet.repository.WalletRepository;
import com.autowash.features.wallet.repository.WalletTransactionRepository;
import com.autowash.features.wallet.enums.WalletTransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserService userService;
    private final PayOS payOS;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    @Value("${payos.test-mode:false}")
    private boolean payosTestMode;

    @Value("${payos.test-amount:10000}")
    private long payosTestAmount;

    @Transactional
    public DepositResponse createDepositLink(DepositRequest request) {
        User user = userService.getCurrentUserEntity();
        Integer amount = request.getAmount();
        if (amount == null || amount < 10000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền nạp tối thiểu là 10,000đ");
        }

        // Initialize wallet if not present
        Wallet wallet = walletRepository.findWalletByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder()
                            .user(user)
                            .balance(0)
                            .build();
                    return walletRepository.save(newWallet);
                });

        long unique = System.currentTimeMillis() % 100L;
        long orderCode = 6000000000000000L + (user.getId() * 10000000000L) + ((long) amount * 100L) + unique;

        try {
            String effectiveReturnUrl = getEffectiveUrl(returnUrl, "/payment-success");
            String effectiveCancelUrl = getEffectiveUrl(cancelUrl, "/payment-failed");

            CreatePaymentLinkRequest payosRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(payosTestMode ? payosTestAmount : (long) amount)
                    .description("Nap vi " + user.getId())
                    .returnUrl(effectiveReturnUrl)
                    .cancelUrl(effectiveCancelUrl)
                    .build();

            CreatePaymentLinkResponse payosResponse = payOS.paymentRequests().create(payosRequest);
            return new DepositResponse(payosResponse.getCheckoutUrl());
        } catch (Exception e) {
            log.error("Failed to create PayOS checkout link for user {}: {}", user.getId(), e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể tạo link thanh toán PayOS: " + e.getMessage()
            );
        }
    }

    @Transactional
    public List<WalletTransactionResponse> getTransactions() {
        User user = userService.getCurrentUserEntity();
        
        // Initialize wallet if not present
        Wallet wallet = walletRepository.findWalletByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder()
                            .user(user)
                            .balance(0)
                            .build();
                    return walletRepository.save(newWallet);
                });

        List<WalletTransaction> transactions = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());

        return transactions.stream()
                .map(tx -> WalletTransactionResponse.builder()
                        .id(tx.getId())
                        .amount(tx.getAmount())
                        .transactionType(tx.getTransactionType().name())
                        .description(tx.getDescription())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void verifyDeposit(Long orderCode) {
        log.info("Verifying wallet deposit for orderCode: {}", orderCode);
        if (orderCode == null || orderCode < 5000000000000000L) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giao dịch không hợp lệ cho nạp tiền ví");
        }

        long userId;
        int depositAmount;

        if (orderCode >= 6000000000000000L) {
            long temp = orderCode - 6000000000000000L;
            userId = temp / 10000000000L;
            long remaining = temp % 10000000000L;
            depositAmount = (int) (remaining / 100L);
        } else {
            userId = (orderCode - 5000000000000000L) / 10000000L;
            depositAmount = 0; // Will be set from PayOS response below
        }

        User user = userService.getCurrentUserEntity();
        if (!user.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập giao dịch này");
        }

        try {
            log.info("Fetching payment link info from PayOS for wallet deposit orderCode: {}", orderCode);
            vn.payos.model.v2.paymentRequests.PaymentLink paymentLink = payOS.paymentRequests().get(orderCode);
            String payosStatus = paymentLink.getStatus().toString();
            log.info("PayOS returned status: {} for wallet deposit orderCode: {}", payosStatus, orderCode);

            if (vn.payos.model.v2.paymentRequests.PaymentLinkStatus.PAID.equals(paymentLink.getStatus())) {
                Wallet wallet = walletRepository.findByUserId(userId)
                        .orElseGet(() -> {
                            Wallet newWallet = Wallet.builder()
                                    .user(user)
                                    .balance(0)
                                    .build();
                            return walletRepository.save(newWallet);
                        });

                String txDescription = "Nạp tiền qua PayOS - GD " + orderCode;
                boolean txExists = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId())
                        .stream()
                        .anyMatch(tx -> txDescription.equals(tx.getDescription()));

                if (!txExists) {
                    if (orderCode < 6000000000000000L) {
                        depositAmount = paymentLink.getAmount() != null ? paymentLink.getAmount().intValue() : 0;
                    }
                    wallet.setBalance(wallet.getBalance() + depositAmount);
                    walletRepository.save(wallet);

                    WalletTransaction transaction = WalletTransaction.builder()
                            .wallet(wallet)
                            .amount(depositAmount)
                            .transactionType(WalletTransactionType.DEPOSIT)
                            .description(txDescription)
                            .build();
                    walletTransactionRepository.save(transaction);
                    log.info("Successfully verified and processed wallet deposit for user ID {}, amount: {}", userId, depositAmount);
                }
            } else {
                log.warn("Wallet deposit payment link status is {} - not paid yet.", payosStatus);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giao dịch chưa được thanh toán thành công (Trạng thái: " + payosStatus + ")");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error communicating with PayOS API for wallet deposit orderCode {}: {}", orderCode, e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể xác thực trạng thái thanh toán nạp tiền ví với PayOS: " + e.getMessage()
            );
        }
    }

    private String getEffectiveUrl(String defaultUrl, String path) {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                jakarta.servlet.http.HttpServletRequest req = attrs.getRequest();
                String origin = req.getHeader("Origin");
                if (origin == null || origin.isBlank()) {
                    String referer = req.getHeader("Referer");
                    if (referer != null && !referer.isBlank()) {
                        java.net.URI uri = java.net.URI.create(referer);
                        origin = uri.getScheme() + "://" + uri.getAuthority();
                    }
                }
                if (origin != null && !origin.isBlank()) {
                    return origin.replaceAll("/+$", "") + path;
                }
            }
        } catch (Exception e) {
            log.warn("Could not determine dynamic request origin for PayOS returnUrl: {}", e.getMessage());
        }
        return defaultUrl;
    }
}
