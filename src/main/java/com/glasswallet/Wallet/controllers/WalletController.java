package com.glasswallet.Wallet.controllers;

import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.dtos.requests.CreateWalletRequest;
import com.glasswallet.Wallet.dtos.response.CreateWalletResponse;
import com.glasswallet.Wallet.dtos.response.WalletBalanceResponse;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.Wallet.exceptions.InvalidCredentialsException;
import com.glasswallet.Wallet.service.interfaces.WalletService;
import com.glasswallet.Wallet.utils.PaymentResult;
import com.glasswallet.transaction.dtos.response.DepositResponse;
import com.glasswallet.transaction.dtos.response.WithdrawalResponse;
import com.glasswallet.user.dtos.responses.WalletProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/{userId}/create")
    public CreateWalletResponse createWallet(@PathVariable UUID userId, @RequestBody CreateWalletRequest request) {
        return walletService.createWalletForUser(userId, request);
    }

    @GetMapping("/{userId}")
    public List<Wallet> getUserWallets(@PathVariable UUID userId) {
        return walletService.getWallets(userId);
    }

    @PostMapping("/deposit/fiat")
    public DepositResponse depositFiat(
            @RequestParam UUID receiverId,
            @RequestParam UUID companyId,
            @RequestParam BigDecimal amount,
            @RequestParam String reference
    ) {
        return walletService.depositFiat(receiverId, companyId, amount, reference);
    }

    @PostMapping("/deposit/sui")
    public DepositResponse depositSui(
            @RequestParam UUID receiverId,
            @RequestParam UUID companyId,
            @RequestParam BigDecimal amount,
            @RequestParam String reference
    ) {
        return walletService.depositSui(receiverId, companyId, amount, reference);
    }

    @PostMapping("/withdraw/fiat")
    public WithdrawalResponse withdrawFiat(
            @RequestParam UUID senderId,
            @RequestParam UUID companyId,
            @RequestParam BigDecimal amount,
            @RequestParam String reference
    ) {
        return walletService.withdrawFiat(senderId, companyId, amount, reference);
    }

    @PostMapping("/withdraw/sui")
    public WithdrawalResponse withdrawSui(
            @RequestParam UUID senderId,
            @RequestParam UUID companyId,
            @RequestParam BigDecimal amount,
            @RequestParam String reference
    ) {
        return walletService.withdrawSui(senderId, companyId, amount, reference);
    }

    @GetMapping("/balances")
    public WalletBalanceResponse getWalletBalance(
            @RequestParam String recipientIdentifier,
            @RequestParam String password
    ) throws InvalidCredentialsException {
        return walletService.getUserWalletBalances(recipientIdentifier, password);
    }

    @PostMapping("/receive")
    public PaymentResult receivePayment(
            @RequestParam String recipientIdentifier,
            @RequestParam WalletCurrency currency,
            @RequestParam BigDecimal amount
    ) {
        return walletService.receivePayment(recipientIdentifier, currency, amount);
    }

    @GetMapping("/profile/{userId}")
    public WalletProfileDto getWalletProfile(@PathVariable UUID userId) {
        return walletService.getProfile(userId);
    }

    @GetMapping("/details/{walletId}")
    public Wallet getWalletById(@PathVariable UUID walletId) {
        return walletService.getWalletById(walletId);
    }
}
