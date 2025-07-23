package com.glasswallet.Wallet.service.interfaces;

import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.dtos.requests.CreateWalletRequest;
import com.glasswallet.Wallet.dtos.response.CreateWalletResponse;
import com.glasswallet.Wallet.dtos.response.WalletBalanceResponse;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.Wallet.exceptions.InvalidCredentialsException;
import com.glasswallet.Wallet.utils.PaymentResult;
import com.glasswallet.transaction.data.models.Transaction;
import com.glasswallet.transaction.dtos.response.DepositResponse;
import com.glasswallet.transaction.dtos.response.WithdrawalResponse;
import com.glasswallet.transaction.enums.TransactionType;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.dtos.responses.WalletProfileDto;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {


    CreateWalletResponse createWalletForUser(UUID userId, CreateWalletRequest request);

    void createWalletIfNotExists(User user);


    CreateWalletResponse createWallet(User user);

    List<Wallet> getWallets(UUID userId);

    DepositResponse depositFiat(UUID receiverId, UUID companyId, BigDecimal amount, String reference);

    DepositResponse depositSui(UUID receiverId, UUID companyId, BigDecimal amount, String reference);

    WithdrawalResponse withdrawFiat(UUID senderId, UUID companyId, BigDecimal amount, String reference);

    WithdrawalResponse withdrawSui(UUID senderId, UUID companyId, BigDecimal amount, String reference);

    WalletBalanceResponse getUserWalletBalances(String recipientIdentifier, String password) throws InvalidCredentialsException;

    @Transactional
    PaymentResult receivePayment(String recipientIdentifier, WalletCurrency currency, BigDecimal amount);

    WalletProfileDto getProfile(UUID id);

    Wallet getWalletById(UUID walletId);
}
