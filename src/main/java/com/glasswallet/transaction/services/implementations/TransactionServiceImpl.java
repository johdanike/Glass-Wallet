package com.glasswallet.transaction.services.implementations;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.dtos.requests.LogTransactionRequest;
import com.glasswallet.Ledger.dtos.responses.SuiResponse;
import com.glasswallet.Ledger.service.interfaces.LedgerService;
import com.glasswallet.Ledger.service.interfaces.MoveServiceClient;
import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.data.repositories.WalletRepository;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.Wallet.exceptions.InsufficientBalanceException;
import com.glasswallet.Wallet.exceptions.WalletNotFoundException;
import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.platform.data.repositories.PlatformUserRepository;
import com.glasswallet.platform.exceptions.NotFoundException;
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
import com.glasswallet.transaction.enums.TransactionStatus;
import com.glasswallet.transaction.enums.TransactionType;
import com.glasswallet.transaction.services.interfaces.TransactionService;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import com.glasswallet.user.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final LedgerService ledgerService;
    private final TransactionRepository transactionRepository;
    private final MoveServiceClient moveServiceClient;
    private final UserRepository userRepository;
    private final PlatformUserRepository platformUserRepository;
    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public DepositResponse processDeposit(DepositRequest request) {
        checkAmountPositive(request.getAmount());

        if (request.getCurrency() == WalletCurrency.SUI) {
            Wallet wallet = getWalletWithLock(request.getReceiverId(), request.getCurrency());
            wallet.setBalance(wallet.getBalance().add(request.getAmount()));
            walletRepository.save(wallet);
        } else {
            PlatformUser user = getPlatformUserWithLock(request.getReceiverId().toString());
            user.setBalanceFiat(user.getBalanceFiat().add(request.getAmount()));
            platformUserRepository.save(user);
        }

        LedgerEntry ledger = ledgerService.logDeposit(request);
        SuiResponse suiResponse = moveServiceClient.logOnChain(ledger);

        transactionRepository.save(buildTransaction(ledger, suiResponse, TransactionType.DEPOSIT));

        DepositResponse response = new DepositResponse();
        response.setMessage("Deposit successful.");
        return response;
    }

    @Override
    @Transactional
    public WithdrawalResponse processWithdrawal(WithdrawalRequest request) {
        checkAmountPositive(request.getAmount());

        if (WalletCurrency.valueOf(request.getCurrency()) == WalletCurrency.SUI) {
            Wallet wallet = getWalletWithLock(UUID.fromString(request.getSenderId()), WalletCurrency.SUI);
            validateBalance(wallet.getBalance(), request.getAmount(), "Insufficient SUI balance.");
            wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
            walletRepository.save(wallet);
        } else {
            PlatformUser user = getPlatformUserWithLock(request.getSenderId());
            validateBalance(user.getBalanceFiat(), request.getAmount(), "Insufficient fiat balance.");
            user.setBalanceFiat(user.getBalanceFiat().subtract(request.getAmount()));
            platformUserRepository.save(user);
        }

        LedgerEntry ledger = ledgerService.logWithdrawal(request);
        SuiResponse suiResponse = moveServiceClient.logOnChain(ledger);

        transactionRepository.save(buildTransaction(ledger, suiResponse, TransactionType.WITHDRAWAL));

        WithdrawalResponse response = new WithdrawalResponse();
        response.setMessage("Withdrawal successful.");
        return response;
    }

    @Override
    @Transactional
    public TransferResponse processTransfer(TransferRequest request) {
        WalletCurrency currency = WalletCurrency.valueOf(request.getCurrency());
        checkTransactionAndCurrencyType(currency, TransactionType.valueOf(currency == WalletCurrency.SUI ? "CRYPTO_TRANSFER" : "FIAT_TRANSFER"));

        Transaction tx = transact(
                UUID.fromString(request.getSenderId()),
                UUID.fromString(request.getReceiverId()),
                UUID.fromString(request.getCompanyId()),
                currency == WalletCurrency.SUI ? TransactionType.CRYPTO_TRANSFER : TransactionType.FIAT_TRANSFER,
                currency,
                request.getReference(),
                request.getAmount()
        );

        TransferResponse response = new TransferResponse();
        response.setMessage("Transfer successful.");

        return response;
    }

    @Override
    @Transactional
    public BulkDisbursementResponse processBulkDisbursement(BulkDisbursementRequest request) {
        checkAmountPositive(request.getTotalAmount());
        getPlatformUserWithLock(request.getSenderId());

        List<LedgerEntry> ledgerEntries = ledgerService.logBulkDisbursement(request);

        for (LedgerEntry entry : ledgerEntries) {
            SuiResponse suiResponse = moveServiceClient.logOnChain(entry);
            if (WalletCurrency.valueOf(entry.getCurrency()) == WalletCurrency.SUI) {
                Wallet wallet = getWalletWithLock(UUID.fromString(entry.getReceiverId()), WalletCurrency.SUI);
                wallet.setBalance(wallet.getBalance().add(entry.getAmount()));
                walletRepository.save(wallet);
            } else {
                PlatformUser user = getPlatformUserWithLock(entry.getReceiverId());
                user.setBalanceFiat(user.getBalanceFiat().add(entry.getAmount()));
                platformUserRepository.save(user);
            }

            transactionRepository.save(buildTransaction(entry, suiResponse, TransactionType.BULK_DISBURSEMENT));
        }

        BulkDisbursementResponse response = new BulkDisbursementResponse();
        response.setMessage("Bulk disbursement successful.");
        return response;
    }

    @Override
    public List<Transaction> getAllTransactionsForUser(String userId) {
        return transactionRepository.findBySenderIdOrReceiverId(userId, userId);
    }

    @Override
    public List<Transaction> getTransactionsByCompany(String companyId) {
        return transactionRepository.findAllByPlatformId(companyId);
    }

    @Override
    public Optional<Transaction> getTransactionById(UUID txId) {
        return transactionRepository.findById(txId);
    }

    @Transactional
    @Override
    public Transaction transact(UUID senderId, UUID receiverId, UUID companyId, TransactionType type, WalletCurrency currency, String reference, BigDecimal amount) {
        checkAmountPositive(amount);
        checkTransactionAndCurrencyType(currency, type);

        fetchSenderAndReceiver data = getSenderAndReceiver(senderId, receiverId, currency);
        validateBalance(data.senderWallet().getBalance(), amount, "Insufficient balance");

        LedgerEntry ledger = ledgerService.logTransaction(LogTransactionRequest.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .companyId(companyId)
                .type(type)
                .currency(currency)
                .amount(amount)
                .referenceId(reference)
                .build());

        data.senderWallet().setBalance(data.senderWallet().getBalance().subtract(amount));
        data.receiverWallet().setBalance(data.receiverWallet().getBalance().add(amount));
        walletRepository.saveAll(List.of(data.senderWallet(), data.receiverWallet()));

        return logOnChainAndSaveTransaction(type, ledger, data);
    }

    private Transaction logOnChainAndSaveTransaction(TransactionType type, LedgerEntry ledger, fetchSenderAndReceiver data) {
        SuiResponse suiResponse = null;
        try {
            suiResponse = moveServiceClient.logOnChain(ledger);
        } catch (Exception e) {
            log.warn("Failed to log transaction on-chain: {}", e.getMessage());
        }

        Transaction tx = Transaction.builder()
                .id(UUID.fromString(ledger.getReference()))
                .senderId(data.sender().getId().toString())
                .receiverId(data.receiver().getId().toString())
                .platformId(ledger.getCompanyId())
                .transactionType(type)
                .amount(ledger.getAmount())
                .currency(WalletCurrency.valueOf(ledger.getCurrency()))
                .status(TransactionStatus.SUCCESSFUL)
                .timestamp(Instant.now())
                .gasFee(suiResponse != null ? suiResponse.getGasFee() : null)
                .onChain(suiResponse != null)
                .build();

        return transactionRepository.save(tx);
    }

    private void checkAmountPositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
    }

    private void validateBalance(BigDecimal balance, BigDecimal amount, String errorMessage) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(errorMessage);
        }
    }

    private void checkTransactionAndCurrencyType(WalletCurrency currency, TransactionType type) {
        if (type == TransactionType.CRYPTO_TRANSFER && currency != WalletCurrency.SUI) {
            throw new IllegalArgumentException("CRYPTO_TRANSFER must use SUI wallet");
        }
        if (type == TransactionType.FIAT_TRANSFER && currency != WalletCurrency.NGN) {
            throw new IllegalArgumentException("FIAT_TRANSFER must use NGN wallet");
        }
    }

    private fetchSenderAndReceiver getSenderAndReceiver(UUID senderId, UUID receiverId, WalletCurrency currency) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new UserNotFoundException("Receiver not found"));

        Wallet senderWallet = walletRepository.findByUserAndCurrencyTypeWithLock(sender, currency)
                .orElseThrow(() -> new WalletNotFoundException("Sender wallet not found"));
        Wallet receiverWallet = walletRepository.findByUserAndCurrencyTypeWithLock(receiver, currency)
                .orElseThrow(() -> new WalletNotFoundException("Receiver wallet not found"));

        return new fetchSenderAndReceiver(sender, receiver, senderWallet, receiverWallet);
    }

    private PlatformUser getPlatformUserWithLock(String platformUserId) {
        return (PlatformUser) platformUserRepository.findByIdWithPessimisticLock(UUID.fromString(platformUserId))
                .orElseThrow(() -> new NotFoundException("PlatformUser not found"));
    }

    private Wallet getWalletWithLock(UUID userId, WalletCurrency currency) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return walletRepository.findByUserAndCurrencyTypeWithLock(user, currency)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
    }

    private Transaction buildTransaction(LedgerEntry ledger, SuiResponse suiResponse, TransactionType type) {
        return Transaction.builder()
                .id(UUID.fromString(ledger.getReference()))
                .senderId(ledger.getSenderId())
                .receiverId(ledger.getReceiverId())
                .platformId(ledger.getCompanyId())
                .transactionType(type)
                .currency(WalletCurrency.valueOf(ledger.getCurrency()))
                .amount(ledger.getAmount())
                .status(TransactionStatus.SUCCESSFUL)
                .timestamp(Instant.now())
                .gasFee(suiResponse != null ? suiResponse.getGasFee() : null)
                .onChain(suiResponse != null)
                .build();
    }

    private record fetchSenderAndReceiver(User sender, User receiver, Wallet senderWallet, Wallet receiverWallet) {
    }
}