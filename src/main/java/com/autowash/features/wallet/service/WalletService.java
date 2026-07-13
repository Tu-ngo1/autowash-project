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

        long orderCode = 5000000000000000L + (user.getId() * 10000000L) + (System.currentTimeMillis() % 10000000L);

        try {
            CreatePaymentLinkRequest payosRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(payosTestMode ? payosTestAmount : (long) amount)
                    .description("Nap vi " + user.getId())
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
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
}
