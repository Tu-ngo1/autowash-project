package com.autowash.features.wallet.controller;

import com.autowash.features.wallet.dto.request.DepositRequest;
import com.autowash.features.wallet.dto.response.DepositResponse;
import com.autowash.features.wallet.dto.response.WalletTransactionResponse;
import com.autowash.features.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/deposit")
    public DepositResponse deposit(@RequestBody DepositRequest request) {
        return walletService.createDepositLink(request);
    }

    @GetMapping("/transactions")
    public List<WalletTransactionResponse> getTransactions() {
        return walletService.getTransactions();
    }

    @PostMapping("/verify-payment/{orderCode}")
    public void verifyPayment(@PathVariable Long orderCode) {
        walletService.verifyDeposit(orderCode);
    }
}
