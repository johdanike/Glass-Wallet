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
import com.glasswallet.transaction.dtos.request.BulkDisbursementRequest;
import com.glasswallet.transaction.dtos.request.DepositRequest;
import com.glasswallet.transaction.dtos.request.TransferRequest;
import com.glasswallet.transaction.dtos.request.WithdrawalRequest;
import com.glasswallet.transaction.dtos.response.BulkDisbursementResponse;
import com.glasswallet.transaction.dtos.response.DepositResponse;
import com.glasswallet.transaction.dtos.response.TransferResponse;
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
import java.util.ArrayList;
import java.util.List;
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
        Wallet centralWallet = new Wallet();
        centralWallet.setWalletAddress("0xCentralSuiWalletAddress");
        centralWallet.setBalance(BigDecimal.valueOf(1000));
        centralWallet.setCurrencyType(WalletCurrency.SUI);
        when(walletRepository.findByWalletAddress("0xCentralSuiWalletAddress")).thenReturn(Optional.of(centralWallet));

        PlatformUser centralPool = new PlatformUser();
        centralPool.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        centralPool.setBalanceFiat(BigDecimal.valueOf(1000));
        when(platformUserRepository.findByIdWithPessimisticLock(centralPool.getId())).thenReturn(Optional.of(centralPool));
    }

    @Test
    void processDeposit_suiWallet_success() {
        DepositRequest request = new DepositRequest();
        UUID receiverId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency(WalletCurrency.SUI);
        request.setReceiverId(receiverId);
        PlatformUser user = new PlatformUser();
        user.setId(receiverId);
        user.setBalanceSui(BigDecimal.ZERO);
        LedgerEntry ledger = LedgerEntry.builder().reference(UUID.randomUUID().toString()).build();
        when(platformUserRepository.findByIdWithPessimisticLock(receiverId)).thenReturn(Optional.of(user));
        when(ledgerService.logDeposit(any())).thenReturn(ledger);
        when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());
        when(transactionRepository.save(any())).thenReturn(new Transaction());

        DepositResponse response = transactionService.processDeposit(request);
        assertEquals("Deposit successful.", response.getMessage());
        assertEquals(BigDecimal.valueOf(100), user.getBalanceSui());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void processDeposit_negativeAmount_throwsException() {
        DepositRequest request = new DepositRequest();
        request.setAmount(BigDecimal.valueOf(-100));
        assertThrows(IllegalArgumentException.class, () -> transactionService.processDeposit(request));
    }

    @Test
    void processDeposit_nullRequest_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> transactionService.processDeposit(null));
    }

    @Test
    void processDeposit_zeroAmount_throwsException() {
        DepositRequest request = new DepositRequest();
        UUID receiverId = UUID.randomUUID();
        request.setAmount(BigDecimal.ZERO);
        request.setCurrency(WalletCurrency.SUI);
        request.setReceiverId(receiverId);
        assertThrows(IllegalArgumentException.class, () -> transactionService.processDeposit(request));
    }

    @Test
    void processDeposit_unsupportedCurrency_throwsException() {
        DepositRequest request = new DepositRequest();
        UUID receiverId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency(null);
        request.setReceiverId(receiverId);
        assertThrows(IllegalArgumentException.class, () -> transactionService.processDeposit(request));
    }

    @Test
    void processDeposit_nonExistentUser_success() {
        DepositRequest request = new DepositRequest();
        UUID receiverId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency(WalletCurrency.SUI);
        request.setReceiverId(receiverId);
        when(platformUserRepository.findByIdWithPessimisticLock(receiverId)).thenReturn(Optional.empty());
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(new User()));
        PlatformUser newUser = new PlatformUser();
        newUser.setId(receiverId);
        newUser.setBalanceSui(BigDecimal.ZERO);
        when(platformUserRepository.save(any(PlatformUser.class))).thenReturn(newUser);
        when(transactionRepository.save(any())).thenReturn(new Transaction());
        when(ledgerService.logDeposit(any())).thenReturn(LedgerEntry.builder().reference(UUID.randomUUID().toString()).build());
        when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());

        DepositResponse response = transactionService.processDeposit(request);
        assertEquals("Deposit successful.", response.getMessage());
    }

    @Test
    void processDeposit_walletLockFailure_throwsException() {
        DepositRequest request = new DepositRequest();
        UUID receiverId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency(WalletCurrency.SUI);
        request.setReceiverId(receiverId);
        when(platformUserRepository.findByIdWithPessimisticLock(receiverId)).thenReturn(Optional.empty());
        when(userRepository.findById(receiverId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> transactionService.processDeposit(request));
    }

    @Test
    void processDeposit_nullCurrency_throwsException() {
        DepositRequest request = new DepositRequest();
        UUID receiverId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency(null);
        request.setReceiverId(receiverId);
        assertThrows(IllegalArgumentException.class, () -> transactionService.processDeposit(request));
    }

    @Test
    void processDeposit_nullReceiverId_throwsException() {
        DepositRequest request = new DepositRequest();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency(WalletCurrency.SUI);
        request.setReceiverId(null);
        assertThrows(IllegalArgumentException.class, () -> transactionService.processDeposit(request));
    }

    @Test
    void processDeposit_ledgerServiceFailure_throwsException() {
        DepositRequest request = new DepositRequest();
        UUID receiverId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency(WalletCurrency.SUI);
        request.setReceiverId(receiverId);
        PlatformUser user = new PlatformUser();
        user.setId(receiverId);
        user.setBalanceSui(BigDecimal.ZERO);
        when(platformUserRepository.findByIdWithPessimisticLock(receiverId)).thenReturn(Optional.of(user));
        when(ledgerService.logDeposit(any())).thenThrow(new RuntimeException("Ledger error"));
        assertThrows(RuntimeException.class, () -> transactionService.processDeposit(request));
    }

    @Test
    void processDeposit_moveServiceClientFailure_throwsException() {
        DepositRequest request = new DepositRequest();
        UUID receiverId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency(WalletCurrency.SUI);
        request.setReceiverId(receiverId);
        PlatformUser user = new PlatformUser();
        user.setId(receiverId);
        user.setBalanceSui(BigDecimal.ZERO);
        LedgerEntry ledger = LedgerEntry.builder().reference(UUID.randomUUID().toString()).build();
        when(platformUserRepository.findByIdWithPessimisticLock(receiverId)).thenReturn(Optional.of(user));
        when(ledgerService.logDeposit(any())).thenReturn(ledger);
        when(moveServiceClient.logOnChain(any())).thenThrow(new RuntimeException("Move error"));
        assertThrows(RuntimeException.class, () -> transactionService.processDeposit(request));
    }

    @Test
    void processDeposit_largeAmount_success() {
        DepositRequest request = new DepositRequest();
        UUID receiverId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(1_000_000));
        request.setCurrency(WalletCurrency.SUI);
        request.setReceiverId(receiverId);
        PlatformUser user = new PlatformUser();
        user.setId(receiverId);
        user.setBalanceSui(BigDecimal.ZERO);
        LedgerEntry ledger = LedgerEntry.builder().reference(UUID.randomUUID().toString()).build();
        when(platformUserRepository.findByIdWithPessimisticLock(receiverId)).thenReturn(Optional.of(user));
        when(ledgerService.logDeposit(any())).thenReturn(ledger);
        when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());
        when(transactionRepository.save(any())).thenReturn(new Transaction());

        DepositResponse response = transactionService.processDeposit(request);
        assertEquals("Deposit successful.", response.getMessage());
        assertEquals(BigDecimal.valueOf(1_000_000), user.getBalanceSui());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void processDeposit_decimalAmount_success() {
        DepositRequest request = new DepositRequest();
        UUID receiverId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(123.45));
        request.setCurrency(WalletCurrency.SUI);
        request.setReceiverId(receiverId);
        PlatformUser user = new PlatformUser();
        user.setId(receiverId);
        user.setBalanceSui(BigDecimal.ZERO);
        LedgerEntry ledger = LedgerEntry.builder().reference(UUID.randomUUID().toString()).build();
        when(platformUserRepository.findByIdWithPessimisticLock(receiverId)).thenReturn(Optional.of(user));
        when(ledgerService.logDeposit(any())).thenReturn(ledger);
        when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());
        when(transactionRepository.save(any())).thenReturn(new Transaction());

        DepositResponse response = transactionService.processDeposit(request);
        assertEquals("Deposit successful.", response.getMessage());
        assertEquals(BigDecimal.valueOf(123.45), user.getBalanceSui());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void processWithdrawal_insufficientFiatBalance_throwsException() {
        WithdrawalRequest request = new WithdrawalRequest();
        UUID senderId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("NGN");
        request.setSenderId(String.valueOf(senderId));
        PlatformUser user = new PlatformUser();
        user.setId(senderId);
        user.setBalanceFiat(BigDecimal.ZERO);
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.of(user));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.processWithdrawal(request));
    }

    @Test
    void processWithdrawal_successForFiat() {
        WithdrawalRequest request = new WithdrawalRequest();
        UUID senderId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(50));
        request.setCurrency("NGN");
        request.setSenderId(String.valueOf(senderId));
        PlatformUser user = new PlatformUser();
        user.setId(senderId);
        user.setBalanceFiat(BigDecimal.valueOf(100));
        LedgerEntry ledger = LedgerEntry.builder().reference(UUID.randomUUID().toString()).senderId(senderId.toString()).build();
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.of(user));
        when(ledgerService.logWithdrawal(any())).thenReturn(ledger);
        when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());
        when(transactionRepository.save(any())).thenReturn(new Transaction());

        WithdrawalResponse response = transactionService.processWithdrawal(request);
        assertEquals("Withdrawal successful.", response.getMessage());
        assertEquals(BigDecimal.valueOf(50), user.getBalanceFiat());
        verify(platformUserRepository).save(any(PlatformUser.class));
    }

    @Test
    void processWithdrawal_invalidCurrency_throwsException() {
        WithdrawalRequest request = new WithdrawalRequest();
        UUID senderId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(10));
        request.setCurrency("BTC");
        request.setSenderId(String.valueOf(senderId));
        assertThrows(IllegalArgumentException.class, () -> transactionService.processWithdrawal(request));
    }

    @Test
    void processWithdrawal_nullRequest_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> transactionService.processWithdrawal(null));
    }

    @Test
    void processWithdrawal_zeroAmount_throwsException() {
        WithdrawalRequest request = new WithdrawalRequest();
        UUID senderId = UUID.randomUUID();
        request.setAmount(BigDecimal.ZERO);
        request.setCurrency("NGN");
        request.setSenderId(String.valueOf(senderId));
        assertThrows(IllegalArgumentException.class, () -> transactionService.processWithdrawal(request));
    }

    @Test
    void processWithdrawal_negativeAmount_throwsException() {
        WithdrawalRequest request = new WithdrawalRequest();
        UUID senderId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(-10));
        request.setCurrency("NGN");
        request.setSenderId(String.valueOf(senderId));
        assertThrows(IllegalArgumentException.class, () -> transactionService.processWithdrawal(request));
    }

    @Test
    void processWithdrawal_nullSenderId_throwsException() {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setAmount(BigDecimal.valueOf(10));
        request.setCurrency("NGN");
        request.setSenderId(null);
        assertThrows(IllegalArgumentException.class, () -> transactionService.processWithdrawal(request));
    }

    @Test
    void processWithdrawal_nonExistentUser_throwsException() {
        WithdrawalRequest request = new WithdrawalRequest();
        UUID senderId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(10));
        request.setCurrency("NGN");
        request.setSenderId(String.valueOf(senderId));
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.empty());
        when(userRepository.findById(senderId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> transactionService.processWithdrawal(request));
    }

    @Test
    void processWithdrawal_exactBalance_success() {
        WithdrawalRequest request = new WithdrawalRequest();
        UUID senderId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("NGN");
        request.setSenderId(String.valueOf(senderId));
        PlatformUser user = new PlatformUser();
        user.setId(senderId);
        user.setBalanceFiat(BigDecimal.valueOf(100));
        LedgerEntry ledger = LedgerEntry.builder().reference(UUID.randomUUID().toString()).senderId(senderId.toString()).build();
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.of(user));
        when(ledgerService.logWithdrawal(any())).thenReturn(ledger);
        when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());
        when(transactionRepository.save(any())).thenReturn(new Transaction());

        WithdrawalResponse response = transactionService.processWithdrawal(request);
        assertEquals("Withdrawal successful.", response.getMessage());
        assertEquals(BigDecimal.ZERO, user.getBalanceFiat());
        verify(platformUserRepository).save(any(PlatformUser.class));
    }

    @Test
    void processWithdrawal_decimalAmount_success() {
        WithdrawalRequest request = new WithdrawalRequest();
        UUID senderId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(12.34));
        request.setCurrency("NGN");
        request.setSenderId(String.valueOf(senderId));
        PlatformUser user = new PlatformUser();
        user.setId(senderId);
        user.setBalanceFiat(BigDecimal.valueOf(100));
        LedgerEntry ledger = LedgerEntry.builder().reference(UUID.randomUUID().toString()).senderId(senderId.toString()).build();
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.of(user));
        when(ledgerService.logWithdrawal(any())).thenReturn(ledger);
        when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());
        when(transactionRepository.save(any())).thenReturn(new Transaction());

        WithdrawalResponse response = transactionService.processWithdrawal(request);
        assertEquals("Withdrawal successful.", response.getMessage());
        assertEquals(BigDecimal.valueOf(87.66), user.getBalanceFiat());
        verify(platformUserRepository).save(any(PlatformUser.class));
    }


    @Test
    void processTransfer_nullRequest_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> transactionService.processTransfer(null));
    }

    @Test
    void processTransfer_negativeAmount_throwsException() {
        TransferRequest request = new TransferRequest();
        request.setAmount(BigDecimal.valueOf(-10));
        assertThrows(IllegalArgumentException.class, () -> transactionService.processTransfer(request));
    }

    @Test
    void processTransfer_zeroAmount_throwsException() {
        TransferRequest request = new TransferRequest();
        request.setAmount(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> transactionService.processTransfer(request));
    }

    @Test
    void processTransfer_unsupportedCurrency_throwsException() {
        TransferRequest request = new TransferRequest();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        request.setAmount(BigDecimal.TEN);
        request.setSenderId(String.valueOf(senderId));
        request.setReceiverId(String.valueOf(receiverId));
        request.setCompanyId(String.valueOf(companyId));
        request.setCurrency("BTC");
        assertThrows(IllegalArgumentException.class, () -> transactionService.processTransfer(request));
    }

    @Test
    void processTransfer_nonExistentSender_throwsException() {
        TransferRequest request = new TransferRequest();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        request.setAmount(BigDecimal.TEN);
        request.setSenderId(String.valueOf(senderId));
        request.setReceiverId(String.valueOf(receiverId));
        request.setCompanyId(String.valueOf(companyId));
        request.setCurrency(WalletCurrency.SUI.name());
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.empty());
        when(userRepository.findById(senderId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> transactionService.processTransfer(request));
    }

    @Test
    void processTransfer_nonExistentReceiver_throwsException() {
        TransferRequest request = new TransferRequest();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        request.setAmount(BigDecimal.TEN);
        request.setSenderId(String.valueOf(senderId));
        request.setReceiverId(String.valueOf(receiverId));
        request.setCompanyId(String.valueOf(companyId));
        request.setCurrency(WalletCurrency.SUI.name());
        PlatformUser sender = new PlatformUser();
        sender.setId(senderId);
        sender.setBalanceSui(BigDecimal.valueOf(100));
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.of(sender));
        when(platformUserRepository.findByIdWithPessimisticLock(receiverId)).thenReturn(Optional.empty());
        when(userRepository.findById(receiverId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> transactionService.processTransfer(request));
    }

    @Test
    void processTransfer_insufficientSenderBalance_throwsException() {
        TransferRequest request = new TransferRequest();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        request.setAmount(BigDecimal.TEN);
        request.setSenderId(String.valueOf(senderId));
        request.setReceiverId(String.valueOf(receiverId));
        request.setCompanyId(String.valueOf(companyId));
        request.setCurrency(WalletCurrency.SUI.name());
        PlatformUser sender = new PlatformUser();
        sender.setId(senderId);
        sender.setBalanceSui(BigDecimal.ZERO);
        PlatformUser receiver = new PlatformUser();
        receiver.setId(receiverId);
        receiver.setBalanceSui(BigDecimal.ZERO);
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.of(sender));
        when(platformUserRepository.findByIdWithPessimisticLock(receiverId)).thenReturn(Optional.of(receiver));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.processTransfer(request));
    }

    @Test
    void processTransfer_exactSenderBalance_success() {
        TransferRequest request = new TransferRequest();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        request.setAmount(BigDecimal.TEN);
        request.setSenderId(String.valueOf(senderId));
        request.setReceiverId(String.valueOf(receiverId));
        request.setCompanyId(String.valueOf(companyId));
        request.setCurrency(WalletCurrency.SUI.name());
        PlatformUser sender = new PlatformUser();
        sender.setId(senderId);
        sender.setBalanceSui(BigDecimal.TEN);
        PlatformUser receiver = new PlatformUser();
        receiver.setId(receiverId);
        receiver.setBalanceSui(BigDecimal.ZERO);
        LedgerEntry ledger = LedgerEntry.builder()
                .reference(UUID.randomUUID().toString())
                .senderId(senderId.toString())
                .receiverId(receiverId.toString())
                .companyId(companyId.toString())
                .amount(BigDecimal.TEN)
                .currency("SUI")
                .build();
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.of(sender));
        when(platformUserRepository.findByIdWithPessimisticLock(receiverId)).thenReturn(Optional.of(receiver));
        when(ledgerService.logTransaction(any(LogTransactionRequest.class))).thenReturn(ledger);
        when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());
        when(transactionRepository.save(any())).thenReturn(new Transaction());

        TransferResponse response = transactionService.processTransfer(request);
        assertEquals("Transfer successful.", response.getMessage());
        assertEquals(BigDecimal.ZERO, sender.getBalanceSui());
        assertEquals(BigDecimal.TEN, receiver.getBalanceSui());
        verify(platformUserRepository).saveAll(anyList());
    }

    @Test
    void processTransfer_decimalAmount_success() {
        TransferRequest request = new TransferRequest();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        request.setAmount(BigDecimal.valueOf(12.34));
        request.setSenderId(String.valueOf(senderId));
        request.setReceiverId(String.valueOf(receiverId));
        request.setCompanyId(String.valueOf(companyId));
        request.setCurrency(WalletCurrency.SUI.name());
        PlatformUser sender = new PlatformUser();
        sender.setId(senderId);
        sender.setBalanceSui(BigDecimal.valueOf(100));
        PlatformUser receiver = new PlatformUser();
        receiver.setId(receiverId);
        receiver.setBalanceSui(BigDecimal.ZERO);
        LedgerEntry ledger = LedgerEntry.builder()
                .reference(UUID.randomUUID().toString())
                .senderId(senderId.toString())
                .receiverId(receiverId.toString())
                .companyId(companyId.toString())
                .amount(BigDecimal.valueOf(12.34))
                .currency("SUI")
                .build();
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.of(sender));
        when(platformUserRepository.findByIdWithPessimisticLock(receiverId)).thenReturn(Optional.of(receiver));
        when(ledgerService.logTransaction(any(LogTransactionRequest.class))).thenReturn(ledger);
        when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());
        when(transactionRepository.save(any())).thenReturn(new Transaction());

        TransferResponse response = transactionService.processTransfer(request);
        assertEquals("Transfer successful.", response.getMessage());
        assertEquals(BigDecimal.valueOf(87.66), sender.getBalanceSui());
        assertEquals(BigDecimal.valueOf(12.34), receiver.getBalanceSui());
        verify(platformUserRepository).saveAll(anyList());
    }

    @Test
    void transact_withInsufficientBalance_throwsException() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        PlatformUser sender = new PlatformUser();
        sender.setId(senderId);
        sender.setBalanceSui(BigDecimal.ZERO);
        PlatformUser receiver = new PlatformUser();
        receiver.setId(receiverId);
        receiver.setBalanceSui(BigDecimal.ZERO);
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.of(sender));
        when(platformUserRepository.findByIdWithPessimisticLock(receiverId)).thenReturn(Optional.of(receiver));

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
        PlatformUser sender = new PlatformUser();
        sender.setId(senderId);
        sender.setBalanceSui(BigDecimal.TEN);
        PlatformUser receiver = new PlatformUser();
        receiver.setId(receiverId);
        receiver.setBalanceSui(BigDecimal.ZERO);
        LedgerEntry ledger = LedgerEntry.builder()
                .reference(UUID.randomUUID().toString())
                .senderId(senderId.toString())
                .receiverId(receiverId.toString())
                .companyId(companyId.toString())
                .amount(BigDecimal.ONE)
                .currency("SUI")
                .build();
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.of(sender));
        when(platformUserRepository.findByIdWithPessimisticLock(receiverId)).thenReturn(Optional.of(receiver));
        when(ledgerService.logTransaction(any(LogTransactionRequest.class))).thenReturn(ledger);
        when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());
        when(transactionRepository.save(any())).thenReturn(new Transaction());

        Transaction result = transactionService.transact(senderId, receiverId, companyId, TransactionType.CRYPTO_TRANSFER, WalletCurrency.SUI, "ref", BigDecimal.ONE);
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(9), sender.getBalanceSui());
        assertEquals(BigDecimal.ONE, receiver.getBalanceSui());
        verify(platformUserRepository).saveAll(anyList());
    }

    @Test
    void processBulkDisbursement_successWithMixedCurrencies() {
    // Arrange: Set up the bulk disbursement request
    BulkDisbursementRequest request = new BulkDisbursementRequest();
    UUID senderId = UUID.randomUUID();
    UUID companyId = UUID.randomUUID();
    request.setSenderId(String.valueOf(senderId));
    request.setCompanyId(companyId.toString());

    List<TransferRequest> disbursements = new ArrayList<>();
    UUID receiverId1 = UUID.randomUUID();
    UUID receiverId2 = UUID.randomUUID();

    TransferRequest tr1 = new TransferRequest();
    tr1.setReceiverId(receiverId1.toString());
    tr1.setAmount(BigDecimal.valueOf(100));
    tr1.setCurrency(WalletCurrency.SUI.name());
    tr1.setReference("ref1");
    disbursements.add(tr1);

    TransferRequest tr2 = new TransferRequest();
    tr2.setReceiverId(receiverId2.toString());
    tr2.setAmount(BigDecimal.valueOf(50));
    tr2.setCurrency(WalletCurrency.NGN.name());
    tr2.setReference("ref2");
    disbursements.add(tr2);

    request.setDisbursements(disbursements);
    BigDecimal totalAmount = disbursements.stream()
            .map(TransferRequest::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    request.setAmount(totalAmount);

    // Mock PlatformUser entities
    PlatformUser sender = new PlatformUser();
    sender.setId(senderId);
    sender.setBalanceSui(BigDecimal.valueOf(100)); // Sufficient for 100 SUI
    sender.setBalanceFiat(BigDecimal.valueOf(50)); // Sufficient for 50 NGN

    PlatformUser receiver1 = new PlatformUser();
    receiver1.setId(receiverId1);
    receiver1.setBalanceSui(BigDecimal.ZERO);

    PlatformUser receiver2 = new PlatformUser();
    receiver2.setId(receiverId2);
    receiver2.setBalanceFiat(BigDecimal.ZERO);

    // Mock centralPool
    UUID CENTRAL_POOL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    PlatformUser centralPool = new PlatformUser();
    centralPool.setId(CENTRAL_POOL_ID);
    centralPool.setBalanceFiat(BigDecimal.valueOf(10_000_000)); // Sufficient balance

    // Mock repository to return appropriate PlatformUser based on ID
    when(platformUserRepository.findByIdWithPessimisticLock(any(UUID.class))).thenAnswer(invocation -> {
        UUID id = invocation.getArgument(0);
        if (id.equals(senderId)) return Optional.of(sender);
        if (id.equals(receiverId1)) return Optional.of(receiver1);
        if (id.equals(receiverId2)) return Optional.of(receiver2);
        if (id.equals(CENTRAL_POOL_ID)) return Optional.of(centralPool);
        return Optional.empty();
    });

    // Mock ledgerService.logTransaction for each disbursement
    LedgerEntry ledger1 = LedgerEntry.builder().reference(UUID.randomUUID().toString()).build();
    LedgerEntry ledger2 = LedgerEntry.builder().reference(UUID.randomUUID().toString()).build();
    when(ledgerService.logTransaction(any(LogTransactionRequest.class))).thenReturn(ledger1, ledger2);

    // Mock additional dependencies
    when(moveServiceClient.logOnChain(any())).thenReturn(new SuiResponse());
    when(transactionRepository.save(any())).thenReturn(new Transaction());

    // Act: Execute the bulk disbursement
    BulkDisbursementResponse response = transactionService.processBulkDisbursement(request);

    // Assert: Verify the results
    assertEquals("Bulk disbursement successful.", response.getMessage());
    verify(transactionRepository, times(2)).save(any(Transaction.class));
    assertEquals(BigDecimal.ZERO, sender.getBalanceSui()); // Sender SUI balance deducted
    assertEquals(BigDecimal.ZERO, sender.getBalanceFiat()); // Sender NGN balance deducted
    assertEquals(BigDecimal.valueOf(100), receiver1.getBalanceSui()); // Receiver1 SUI balance updated
    assertEquals(BigDecimal.valueOf(50), receiver2.getBalanceFiat()); // Receiver2 NGN balance updated
}

    @Test
    void processBulkDisbursement_totalAmountMismatch_throwsException() {
        BulkDisbursementRequest request = new BulkDisbursementRequest();
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        request.setSenderId(String.valueOf(senderId));
        request.setCompanyId(String.valueOf(companyId));

        List<TransferRequest> disbursements = new ArrayList<>();
        TransferRequest tr1 = new TransferRequest();
        tr1.setReceiverId(String.valueOf(UUID.randomUUID()));
        tr1.setAmount(BigDecimal.valueOf(100));
        tr1.setCurrency(WalletCurrency.SUI.name());
        disbursements.add(tr1);
        TransferRequest tr2 = new TransferRequest();
        tr2.setReceiverId(String.valueOf(UUID.randomUUID()));
        tr2.setAmount(BigDecimal.valueOf(50));
        tr2.setCurrency(WalletCurrency.NGN.name());
        disbursements.add(tr2);
        request.setDisbursements(disbursements);

        assertThrows(IllegalArgumentException.class, () -> transactionService.processBulkDisbursement(request));
    }

    @Test
    void processBulkDisbursement_nullRequest_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> transactionService.processBulkDisbursement(null));
    }

    @Test
    void processBulkDisbursement_nullSenderId_throwsException() {
        BulkDisbursementRequest request = new BulkDisbursementRequest();
        request.setSenderId(null);
        request.setCompanyId(String.valueOf(UUID.randomUUID()));
        request.setDisbursements(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> transactionService.processBulkDisbursement(request));
    }

    @Test
    void processBulkDisbursement_emptyDisbursements_throwsException() {
        BulkDisbursementRequest request = new BulkDisbursementRequest();
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        request.setSenderId(String.valueOf(senderId));
        request.setCompanyId(String.valueOf(companyId));
        request.setDisbursements(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> transactionService.processBulkDisbursement(request));
    }

    @Test
    void processBulkDisbursement_nullDisbursements_throwsException() {
        BulkDisbursementRequest request = new BulkDisbursementRequest();
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        request.setSenderId(String.valueOf(senderId));
        request.setCompanyId(String.valueOf(companyId));
        request.setDisbursements(null);

        assertThrows(IllegalArgumentException.class, () -> transactionService.processBulkDisbursement(request));
    }

    @Test
    void processBulkDisbursement_invalidCurrency_throwsException() {
        BulkDisbursementRequest request = new BulkDisbursementRequest();
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        request.setSenderId(String.valueOf(senderId));
        request.setCompanyId(String.valueOf(companyId));

        List<TransferRequest> disbursements = new ArrayList<>();
        TransferRequest tr1 = new TransferRequest();
        tr1.setReceiverId(String.valueOf(UUID.randomUUID()));
        tr1.setAmount(BigDecimal.valueOf(100));
        tr1.setCurrency("BTC");
        disbursements.add(tr1);
        request.setDisbursements(disbursements);

        assertThrows(IllegalArgumentException.class, () -> transactionService.processBulkDisbursement(request));
    }

    @Test
    void processBulkDisbursement_negativeTotalAmount_throwsException() {
        BulkDisbursementRequest request = new BulkDisbursementRequest();
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        request.setSenderId(String.valueOf(senderId));
        request.setCompanyId(String.valueOf(companyId));
        request.setDisbursements(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> transactionService.processBulkDisbursement(request));
    }

    @Test
    void processBulkDisbursement_ledgerServiceFailure_throwsException() {
        BulkDisbursementRequest request = new BulkDisbursementRequest();
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        request.setSenderId(String.valueOf(senderId));
        request.setCompanyId(String.valueOf(companyId));

        List<TransferRequest> disbursements = new ArrayList<>();
        TransferRequest tr1 = new TransferRequest();
        tr1.setReceiverId(String.valueOf(UUID.randomUUID()));
        tr1.setAmount(BigDecimal.valueOf(100));
        tr1.setCurrency(WalletCurrency.SUI.name());
        disbursements.add(tr1);
        request.setDisbursements(disbursements);

        PlatformUser sender = new PlatformUser();
        sender.setId(senderId);
        sender.setBalanceSui(BigDecimal.valueOf(100));
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.of(sender));
        when(ledgerService.logBulkDisbursement(any())).thenThrow(new RuntimeException("Ledger error"));

        assertThrows(RuntimeException.class, () -> transactionService.processBulkDisbursement(request));
    }

    @Test
    void processBulkDisbursement_moveServiceClientFailure_throwsException() {
        BulkDisbursementRequest request = new BulkDisbursementRequest();
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        request.setSenderId(String.valueOf(senderId));
        request.setCompanyId(String.valueOf(companyId));

        List<TransferRequest> disbursements = new ArrayList<>();
        TransferRequest tr1 = new TransferRequest();
        tr1.setReceiverId(String.valueOf(UUID.randomUUID()));
        tr1.setAmount(BigDecimal.valueOf(100));
        tr1.setCurrency(WalletCurrency.SUI.name());
        disbursements.add(tr1);
        request.setDisbursements(disbursements);

        PlatformUser sender = new PlatformUser();
        sender.setId(senderId);
        sender.setBalanceSui(BigDecimal.valueOf(100));
        when(platformUserRepository.findByIdWithPessimisticLock(senderId)).thenReturn(Optional.of(sender));
        LedgerEntry ledger = LedgerEntry.builder().reference(UUID.randomUUID().toString()).build();
        when(ledgerService.logBulkDisbursement(any())).thenReturn(List.of(ledger));
        when(moveServiceClient.logOnChain(any())).thenThrow(new RuntimeException("Move error"));

        assertThrows(RuntimeException.class, () -> transactionService.processBulkDisbursement(request));
    }


}