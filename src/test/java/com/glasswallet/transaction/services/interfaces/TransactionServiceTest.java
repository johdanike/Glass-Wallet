package com.glasswallet.transaction.services.interfaces;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.dtos.requests.LogTransactionRequest;
import com.glasswallet.Ledger.dtos.responses.SuiResponse;
import com.glasswallet.Ledger.service.interfaces.LedgerService;
import com.glasswallet.Ledger.service.interfaces.MoveServiceClient;
import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.data.repositories.WalletRepository;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.Wallet.exceptions.InsufficientBalanceException;
import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.platform.data.repositories.PlatformUserRepository;
import com.glasswallet.transaction.data.models.Transaction;
import com.glasswallet.transaction.data.repositories.TransactionRepository;
import com.glasswallet.transaction.dtos.request.DepositRequest;
import com.glasswallet.transaction.dtos.request.WithdrawalRequest;
import com.glasswallet.transaction.dtos.response.DepositResponse;
import com.glasswallet.transaction.dtos.response.WithdrawalResponse;
import com.glasswallet.transaction.enums.TransactionType;
import com.glasswallet.transaction.services.implementations.TransactionServiceImpl;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TransactionServiceTest {

    @Mock private LedgerService ledgerService;
    @Mock private TransactionRepository transactionRepository;
    @Mock private MoveServiceClient moveServiceClient;
    @Mock private UserRepository userRepository;
    @Mock private PlatformUserRepository platformUserRepository;
    @Mock private WalletRepository walletRepository;

    @InjectMocks private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processDeposit_suiWallet_success() {
        DepositRequest request = new DepositRequest();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency(WalletCurrency.SUI);
        request.setReceiverId(UUID.randomUUID());

        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(0));

        when(walletRepository.findByUserAndCurrencyTypeWithLock(any(), eq(WalletCurrency.SUI))).thenReturn(Optional.of(wallet));
        when(userRepository.findById(any())).thenReturn(Optional.of(new User()));
        when(ledgerService.logDeposit(any())).thenReturn(new LedgerEntry());
        when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());

        DepositResponse response = transactionService.processDeposit(request);
        assertEquals("Deposit successful.", response.getMessage());
    }

    @Test
    void processDeposit_negativeAmount_throwsException() {
        DepositRequest request = new DepositRequest();
        request.setAmount(BigDecimal.valueOf(-100));

        assertThrows(IllegalArgumentException.class, () -> transactionService.processDeposit(request));
    }

    @Test
    void processWithdrawal_insufficientFiatBalance_throwsException() {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("NGN");
        request.setSenderId(UUID.randomUUID().toString());

        PlatformUser user = new PlatformUser();
        user.setBalanceFiat(BigDecimal.ZERO);

        when(platformUserRepository.findByIdWithPessimisticLock(any())).thenReturn(Optional.of(user));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.processWithdrawal(request));
    }

    @Test
    void processWithdrawal_successForFiat() {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setAmount(BigDecimal.valueOf(50));
        request.setCurrency("NGN");
        UUID senderId = UUID.randomUUID();
        request.setSenderId(senderId.toString());

        PlatformUser user = new PlatformUser();
        user.setBalanceFiat(BigDecimal.valueOf(100));

        when(platformUserRepository.findByIdWithPessimisticLock(any())).thenReturn(Optional.of(user));
        when(ledgerService.logWithdrawal(any())).thenReturn(new LedgerEntry());
        when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());

        WithdrawalResponse response = transactionService.processWithdrawal(request);
        assertEquals("Withdrawal successful.", response.getMessage());
    }

    @Test
    void processWithdrawal_invalidCurrency_throwsException() {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setCurrency("BTC");

        assertThrows(IllegalArgumentException.class, () -> transactionService.processWithdrawal(request));
    }

    @Test
    void transact_withInsufficientBalance_throwsException() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Wallet senderWallet = new Wallet();
        senderWallet.setBalance(BigDecimal.ZERO);
        Wallet receiverWallet = new Wallet();

        User sender = new User();
        User receiver = new User();

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(walletRepository.findByUserAndCurrencyTypeWithLock(sender, WalletCurrency.SUI)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserAndCurrencyTypeWithLock(receiver, WalletCurrency.SUI)).thenReturn(Optional.of(receiverWallet));

        assertThrows(InsufficientBalanceException.class, () ->
                transactionService.transact(senderId, receiverId, companyId, TransactionType.CRYPTO_TRANSFER, WalletCurrency.SUI, "ref", BigDecimal.TEN)
        );
    }

    @Test
    void transact_invalidCurrencyTypeForFiatTransfer_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.transact(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        TransactionType.FIAT_TRANSFER, WalletCurrency.SUI, "ref", BigDecimal.ONE));
    }

    @Test
    void transact_success_cryptoTransfer() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Wallet senderWallet = new Wallet();
        senderWallet.setBalance(BigDecimal.TEN);
        Wallet receiverWallet = new Wallet();
        receiverWallet.setBalance(BigDecimal.ZERO);

        User sender = new User();
        User receiver = new User();

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(walletRepository.findByUserAndCurrencyTypeWithLock(sender, WalletCurrency.SUI)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserAndCurrencyTypeWithLock(receiver, WalletCurrency.SUI)).thenReturn(Optional.of(receiverWallet));
        when(ledgerService.logTransaction(any(LogTransactionRequest.class))).thenReturn(
                LedgerEntry.builder().reference(UUID.randomUUID().toString()).amount(BigDecimal.ONE).currency("SUI").senderId(senderId.toString()).receiverId(receiverId.toString()).companyId(companyId.toString()).build()
        );
        when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());
        when(transactionRepository.save(any())).thenReturn(new Transaction());

        Transaction result = transactionService.transact(senderId, receiverId, companyId, TransactionType.CRYPTO_TRANSFER, WalletCurrency.SUI, "ref", BigDecimal.ONE);
        assertNotNull(result);
    }
}
