package com.glasswallet.transaction.services.implementations;

import com.glasswallet.Ledger.data.model.LedgerEntry;
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
import com.glasswallet.transaction.enums.TransactionStatus;
import com.glasswallet.transaction.enums.TransactionType;
import com.glasswallet.transaction.services.interfaces.TransactionService;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final UUID CENTRAL_POOL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String CENTRAL_SUI_WALLET_ADDRESS = "0xCentralSuiWalletAddress";

    private final LedgerService ledgerService;
    private final TransactionRepository transactionRepository;
    private final MoveServiceClient moveServiceClient;
    private final UserRepository userRepository;
    private final PlatformUserRepository platformUserRepository;
    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public DepositResponse processDeposit(DepositRequest request) {
        validateDepositRequest(request);
        PlatformUser user = getPlatformUserWithLock(request.getReceiverId().toString());
        if (user == null) {
            throw new IllegalArgumentException("Receiver not found: " + request.getReceiverId());
        }
        updateBalances(request, user);
        LedgerEntry ledger = ledgerService.logDeposit(request);
        List<Transaction> transactions = logOnChainAndSaveTransaction(
                TransactionType.DEPOSIT,
                List.of(ledger),
                request.getReceiverId(),
                null
        );
        transactionRepository.saveAll(transactions);
        return buildDepositResponse(transactions);
    }

    private void validateDepositRequest(DepositRequest request) {
        if (request == null || request.getReceiverId() == null) {
            throw new IllegalArgumentException("Deposit request or receiver ID cannot be null");
        }
        checkAmountPositive(request.getAmount());
    }
    private void updateBalances(DepositRequest request, PlatformUser user) {
        if (request.getCurrency() == WalletCurrency.SUI) {
            Wallet centralWallet = getCentralSuiWallet();
            centralWallet.setBalance(centralWallet.getBalance().add(request.getAmount()));
            user.setBalanceSui(user.getBalanceSui().add(request.getAmount()));
            walletRepository.save(centralWallet);
        } else {
            PlatformUser centralPool = getCentralPool();
            centralPool.setBalanceFiat(centralPool.getBalanceFiat().add(request.getAmount()));
            user.setBalanceFiat(user.getBalanceFiat().add(request.getAmount()));
            platformUserRepository.save(centralPool);
        }
    }
    private DepositResponse buildDepositResponse(List<Transaction> transactions) {
        DepositResponse response = new DepositResponse();
        response.setMessage("Deposit successful.");
        response.setTransactionId(transactions.stream().map(Transaction::getId).collect(Collectors.toList()));
        return response;
    }


    @Override
    @Transactional
    public WithdrawalResponse processWithdrawal(WithdrawalRequest request) {
        validateWithdrawalRequest(request);

        PlatformUser user = getPlatformUserWithLock(request.getSenderId());
        if (user == null) {
            throw new IllegalArgumentException("Sender not found: " + request.getSenderId());
        }

        processWithdrawalBasedOnCurrency(request, user);

        LedgerEntry ledger = ledgerService.logWithdrawal(request);
        List<Transaction> transactions = logOnChainAndSaveTransaction(
                TransactionType.WITHDRAWAL,
                List.of(ledger),
                UUID.fromString(request.getSenderId()),
                null
        );
        transactionRepository.saveAll(transactions);

        return buildWithdrawalResponse(transactions);
    }
    private void validateWithdrawalRequest(WithdrawalRequest request) {
        if (request == null || request.getSenderId() == null) {
            throw new IllegalArgumentException("Withdrawal request or sender ID cannot be null");
        }
        checkAmountPositive(request.getAmount());
    }

    private void processWithdrawalBasedOnCurrency(WithdrawalRequest request, PlatformUser user) {
        WalletCurrency currency = WalletCurrency.valueOf(request.getCurrency());
        if (currency == WalletCurrency.SUI) {
            handleSuiWithdrawal(request, user);
        } else {
            handleFiatWithdrawal(request, user);
        }
    }

    private WithdrawalResponse buildWithdrawalResponse(List<Transaction> transactions) {
        WithdrawalResponse response = new WithdrawalResponse();
        response.setMessage("Withdrawal successful.");
        response.setTransactionId(
                transactions.stream()
                        .map(Transaction::getId)
                        .collect(Collectors.toList())
        );
        return response;
    }

    private void handleSuiWithdrawal(WithdrawalRequest request, PlatformUser user) {
        Wallet centralWallet = getCentralSuiWallet();
        validateBalance(centralWallet.getBalance(), request.getAmount(), "Insufficient SUI balance in central wallet.");
        validateBalance(user.getBalanceSui(), request.getAmount(), "Insufficient SUI balance for user.");
        centralWallet.setBalance(centralWallet.getBalance().subtract(request.getAmount()));
        user.setBalanceSui(user.getBalanceSui().subtract(request.getAmount()));

        walletRepository.save(centralWallet);
    }
    private void handleFiatWithdrawal(WithdrawalRequest request, PlatformUser user) {
        PlatformUser centralPool = getCentralPool();
        validateBalance(centralPool.getBalanceFiat(), request.getAmount(), "Insufficient fiat balance in central pool.");
        validateBalance(user.getBalanceFiat(), request.getAmount(), "Insufficient fiat balance for user.");

        centralPool.setBalanceFiat(centralPool.getBalanceFiat().subtract(request.getAmount()));
        user.setBalanceFiat(user.getBalanceFiat().subtract(request.getAmount()));

        platformUserRepository.save(centralPool);
    }

    @Override
    @Transactional
    public TransferResponse processTransfer(TransferRequest request) {
        validateTransferRequest(request);

        WalletCurrency currency = WalletCurrency.valueOf(request.getCurrency());
        TransactionType transactionType = getTransactionTypeByCurrency(currency);

        UUID senderId = UUID.fromString(request.getSenderId());
        UUID receiverId = UUID.fromString(request.getReceiverId());
        UUID companyId = UUID.fromString(request.getCompanyId());

        PlatformUser receiver = getAndValidateReceiver(receiverId);

        performTransferAndLog(senderId, receiverId, companyId, transactionType, currency, request);

        List<LedgerEntry> ledgerEntries = ledgerService.logTransfer(request);
        if (ledgerEntries.isEmpty()) {
            throw new IllegalStateException("Failed to log transfer entries");
        }

        List<Transaction> transactions = logOnChainAndSaveTransaction(transactionType, ledgerEntries, senderId, receiverId);
        transactionRepository.saveAll(transactions);

        return buildTransferResponse(transactions);
    }
    private PlatformUser getAndValidateReceiver(UUID receiverId) {
        PlatformUser receiver = getPlatformUserWithLock(receiverId.toString());
        if (receiver == null) {
            throw new IllegalArgumentException("Receiver not found: " + receiverId);
        }
        return receiver;
    }

    private void performTransferAndLog(UUID senderId, UUID receiverId, UUID companyId,
                                       TransactionType transactionType, WalletCurrency currency,
                                       TransferRequest request) {
        transact(senderId, receiverId, companyId, transactionType, currency, request.getReference(), request.getAmount());
    }



    private void validateTransferRequest(TransferRequest request) {
        if (request == null || request.getSenderId() == null || request.getReceiverId() == null || request.getCompanyId() == null) {
            throw new IllegalArgumentException("Transfer request or IDs cannot be null");
        }
        checkAmountPositive(request.getAmount());
    }

    private TransactionType getTransactionTypeByCurrency(WalletCurrency currency) {
        return currency == WalletCurrency.SUI ? TransactionType.CRYPTO_TRANSFER : TransactionType.FIAT_TRANSFER;
    }

    private TransferResponse buildTransferResponse(List<Transaction> transactions) {
        TransferResponse response = new TransferResponse();
        response.setMessage("Transfer successful.");
        response.setTransaction(transactions.stream().map(Transaction::getId).collect(Collectors.toList()));
        return response;
    }


    @Override
    @Transactional
    public BulkDisbursementResponse processBulkDisbursement(BulkDisbursementRequest request) {
        validateBulkRequest(request);
        BigDecimal totalAmount = request.getTotalAmount();
        checkAmountPositive(totalAmount);
        Map<String, BigDecimal> totals = validateAndCalculateDisbursementAmounts(request, totalAmount);
        PlatformUser sender = fetchAndLockSender(request.getSenderId());
        updateSenderBalances(sender, totals.get("SUI"), totals.get("FIAT"));
        UUID companyId = UUID.fromString(request.getCompanyId());
        List<LedgerEntry> ledgerEntries = ledgerService.logBulkDisbursement(request);
        if (ledgerEntries.isEmpty()) {
            throw new IllegalStateException("Failed to log bulk disbursement entries");
        }
        executeDisbursements(sender, request.getDisbursements(), companyId);
        List<Transaction> transactions = finalizeTransactions(ledgerEntries, sender.getId());
        updateCentralPools(ledgerEntries);
        return buildBulkDisbursementResponse(transactions);
    }
    private void validateBulkRequest(BulkDisbursementRequest request) {
        if (request == null || request.getSenderId() == null || request.getDisbursements() == null || request.getDisbursements().isEmpty()) {
            throw new IllegalArgumentException("Bulk disbursement request, sender ID, or disbursements cannot be null or empty");
        }
    }
    private PlatformUser fetchAndLockSender(String senderId) {
        PlatformUser sender = getPlatformUserWithLock(senderId);
        if (sender == null) {
            throw new IllegalArgumentException("Sender not found: " + senderId);
        }
        return sender;
    }
    private void executeDisbursements(PlatformUser sender, List<TransferRequest> disbursements, UUID companyId) {
        for (TransferRequest disbursement : disbursements) {
            UUID receiverId = UUID.fromString(disbursement.getReceiverId());
            WalletCurrency currency = WalletCurrency.valueOf(disbursement.getCurrency());
            TransactionType type = currency == WalletCurrency.SUI ? TransactionType.CRYPTO_TRANSFER : TransactionType.FIAT_TRANSFER;

            transactForBulk(sender, receiverId, companyId, type, currency, disbursement.getReference(), disbursement.getAmount());
        }
    }
    private List<Transaction> finalizeTransactions(List<LedgerEntry> entries, UUID senderId) {
        WalletCurrency currency = WalletCurrency.valueOf(entries.get(0).getCurrency());
        TransactionType type = currency == WalletCurrency.SUI ? TransactionType.CRYPTO_TRANSFER : TransactionType.FIAT_TRANSFER;

        List<Transaction> transactions = logOnChainAndSaveTransaction(type, entries, senderId, null);
        transactionRepository.saveAll(transactions);
        return transactions;
    }
    private void updateCentralPools(List<LedgerEntry> entries) {
        for (LedgerEntry entry : entries) {
            WalletCurrency currency = WalletCurrency.valueOf(entry.getCurrency());
            if (currency == WalletCurrency.SUI) {
                Wallet centralWallet = getCentralSuiWallet();
                centralWallet.setBalance(centralWallet.getBalance().add(entry.getAmount()));
                walletRepository.save(centralWallet);
            } else {
                PlatformUser centralPool = getCentralPool();
                centralPool.setBalanceFiat(centralPool.getBalanceFiat().add(entry.getAmount()));
                platformUserRepository.save(centralPool);
            }
        }
    }
    private BulkDisbursementResponse buildBulkDisbursementResponse(List<Transaction> transactions) {
        BulkDisbursementResponse response = new BulkDisbursementResponse();
        response.setMessage("Bulk disbursement successful.");
        response.setTransferResults(transactions.stream().map(Transaction::getId).collect(Collectors.toList()));
        return response;
    }


    private void updateSenderBalances(PlatformUser sender, BigDecimal totalSui, BigDecimal totalFiat) {
        validateBalance(sender.getBalanceSui(), totalSui, "Insufficient SUI balance for sender.");
        validateBalance(sender.getBalanceFiat(), totalFiat, "Insufficient fiat balance for sender.");
        sender.setBalanceSui(sender.getBalanceSui().subtract(totalSui));
        sender.setBalanceFiat(sender.getBalanceFiat().subtract(totalFiat));
        platformUserRepository.save(sender);
    }

    private Map<String, BigDecimal> validateAndCalculateDisbursementAmounts(BulkDisbursementRequest request, BigDecimal totalAmount) {
        BigDecimal sumOfDisbursements = request.getDisbursements().stream()
                .map(TransferRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAmount.compareTo(sumOfDisbursements) != 0) {
            throw new IllegalArgumentException("Total amount must equal the sum of individual disbursement amounts");
        }

        BigDecimal totalSui = BigDecimal.ZERO;
        BigDecimal totalFiat = BigDecimal.ZERO;

        for (TransferRequest disbursement : request.getDisbursements()) {
            WalletCurrency curr = WalletCurrency.valueOf(disbursement.getCurrency());
            if (curr == WalletCurrency.SUI) {
                totalSui = totalSui.add(disbursement.getAmount());
            } else {
                totalFiat = totalFiat.add(disbursement.getAmount());
            }
        }

        Map<String, BigDecimal> totals = new HashMap<>();
        totals.put("SUI", totalSui);
        totals.put("FIAT", totalFiat);
        return totals;
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
    public Optional<Transaction> getTransactionId(UUID txId) {
        return transactionRepository.findById(txId);
    }

    @Transactional
    @Override
    public Transaction transact(UUID senderId, UUID receiverId, UUID companyId, TransactionType type, WalletCurrency currency, String reference, BigDecimal amount) {
        checkAmountPositive(amount);
        checkTransactionAndCurrencyType(currency, type);

        PlatformUser sender = getPlatformUserWithLockOrThrow(senderId.toString(), "Sender");
        PlatformUser receiver = getPlatformUserWithLockOrThrow(receiverId.toString(), "Receiver");

        processTransferBetweenUsers(sender, receiver, currency, amount);

        platformUserRepository.saveAll(List.of(sender, receiver));

        List<LedgerEntry> ledgerEntries = logLedgerEntries(senderId, receiverId, companyId, currency, reference, amount);
        List<Transaction> transactions = logOnChainAndSaveTransaction(type, ledgerEntries, senderId, receiverId);
        transactionRepository.saveAll(transactions);

        return transactions.get(0);
    }
    private PlatformUser getPlatformUserWithLockOrThrow(String userId, String role) {
        PlatformUser user = getPlatformUserWithLock(userId);
        if (user == null) {
            throw new IllegalArgumentException(role + " not found: " + userId);
        }
        return user;
    }

    private void processTransferBetweenUsers(PlatformUser sender, PlatformUser receiver, WalletCurrency currency, BigDecimal amount) {
        if (currency == WalletCurrency.SUI) {
            validateBalance(sender.getBalanceSui(), amount, "Insufficient SUI balance for sender.");
            sender.setBalanceSui(sender.getBalanceSui().subtract(amount));
            receiver.setBalanceSui(receiver.getBalanceSui().add(amount));
        } else {
            validateBalance(sender.getBalanceFiat(), amount, "Insufficient fiat balance for sender.");
            sender.setBalanceFiat(sender.getBalanceFiat().subtract(amount));
            receiver.setBalanceFiat(receiver.getBalanceFiat().add(amount));
        }
    }

    private List<LedgerEntry> logLedgerEntries(UUID senderId, UUID receiverId, UUID companyId, WalletCurrency currency, String reference, BigDecimal amount) {
        TransferRequest request = TransferRequest.builder()
                .senderId(senderId.toString())
                .receiverId(receiverId.toString())
                .companyId(companyId.toString())
                .amount(amount)
                .currency(currency.name())
                .reference(reference)
                .isCrypto(currency == WalletCurrency.SUI)
                .build();

        List<LedgerEntry> entries = ledgerService.logTransfer(request);
        if (entries.isEmpty()) {
            throw new IllegalStateException("Failed to log transfer entries");
        }
        return entries;
    }

    @Transactional
    public Transaction transactForBulk(
            PlatformUser sender,
            UUID receiverId,
            UUID companyId,
            TransactionType type,
            WalletCurrency currency,
            String reference,
            BigDecimal amount
    ) {
        checkAmountPositive(amount);
        checkTransactionAndCurrencyType(currency, type);

        PlatformUser receiver = fetchAndUpdateReceiverBalance(receiverId, currency, amount);
        platformUserRepository.save(receiver);

        TransferRequest request = createTransferRequest(sender, receiverId, companyId, amount, currency, reference);
        List<LedgerEntry> ledgerEntries = ledgerService.logTransfer(request);
        ensureLedgerEntriesLogged(ledgerEntries);

        List<Transaction> transactions = logOnChainAndSaveTransaction(type, ledgerEntries, sender.getId(), receiverId);
        return transactions.get(0);
    }
    private PlatformUser fetchAndUpdateReceiverBalance(UUID receiverId, WalletCurrency currency, BigDecimal amount) {
        PlatformUser receiver = getPlatformUserWithLock(receiverId.toString());
        if (receiver == null) {
            throw new IllegalArgumentException("Receiver not found: " + receiverId);
        }

        if (currency == WalletCurrency.SUI) {
            receiver.setBalanceSui(receiver.getBalanceSui().add(amount));
        } else {
            receiver.setBalanceFiat(receiver.getBalanceFiat().add(amount));
        }
        return receiver;
    }
    private TransferRequest createTransferRequest(
            PlatformUser sender,
            UUID receiverId,
            UUID companyId,
            BigDecimal amount,
            WalletCurrency currency,
            String reference
    ) {
        return TransferRequest.builder()
                .senderId(sender.getId().toString())
                .receiverId(receiverId.toString())
                .companyId(companyId.toString())
                .amount(amount)
                .currency(currency.name())
                .reference(reference)
                .isCrypto(currency == WalletCurrency.SUI)
                .build();
    }
    private void ensureLedgerEntriesLogged(List<LedgerEntry> ledgerEntries) {
        if (ledgerEntries.isEmpty()) {
            throw new IllegalStateException("Failed to log transfer entries");
        }
    }

    private List<Transaction> logOnChainAndSaveTransaction(TransactionType type, List<LedgerEntry> ledgers, UUID senderId, UUID receiverId) {
        return ledgers.stream()
                .map(ledger -> buildTransactionFromLedger(type, ledger, senderId, receiverId))
                .map(transactionRepository::save)
                .collect(Collectors.toList());
    }
    private Transaction buildTransactionFromLedger(TransactionType type, LedgerEntry ledger, UUID senderId, UUID receiverId) {
        UUID platformUserId = resolvePlatformUserId(ledger, senderId);
        SuiResponse suiResponse = attemptOnChainLog(ledger);

        return Transaction.builder()
                .id(UUID.fromString(Optional.ofNullable(ledger.getReference()).orElse(UUID.randomUUID().toString())))
                .senderId(ledger.getSenderId() != null ? ledger.getSenderId() : senderId.toString())
                .receiverId(resolveReceiverId(ledger, receiverId))
                .platformId(ledger.getCompanyId() != null ? ledger.getCompanyId() : UUID.randomUUID().toString())
                .transactionType(type)
                .amount(Optional.ofNullable(ledger.getAmount()).orElse(BigDecimal.ZERO))
                .currency(WalletCurrency.valueOf(Optional.ofNullable(ledger.getCurrency()).orElse("SUI")))
                .status(TransactionStatus.SUCCESSFUL)
                .timestamp(Instant.now())
                .gasFee(suiResponse != null ? suiResponse.getGasFee() : null)
                .onChain(suiResponse != null)
                .build();
    }
    private SuiResponse attemptOnChainLog(LedgerEntry ledger) {
        try {
            return moveServiceClient.logOnChain(ledger);
        } catch (Exception e) {
            log.warn("Failed to log transaction on-chain: {}", e.getMessage());
            return null;
        }
    }
    private UUID resolvePlatformUserId(LedgerEntry ledger, UUID fallbackId) {
        if (ledger.getSenderId() != null) return UUID.fromString(ledger.getSenderId());
        if (ledger.getReceiverId() != null) return UUID.fromString(ledger.getReceiverId());
        return fallbackId;
    }

    private String resolveReceiverId(LedgerEntry ledger, UUID fallbackReceiverId) {
        if (ledger.getReceiverId() != null) return ledger.getReceiverId();
        return fallbackReceiverId != null ? fallbackReceiverId.toString() : null;
    }


    private void checkAmountPositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
    }

    private void validateBalance(BigDecimal balance, BigDecimal amount, String errorMessage) {
        if (balance == null || amount == null || balance.compareTo(amount) < 0) {
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

    private PlatformUser getPlatformUserWithLock(String platformUserId) {
        if (platformUserId == null) {
            throw new IllegalArgumentException("Platform user ID cannot be null");
        }
        return platformUserRepository.findByIdWithPessimisticLock(UUID.fromString(platformUserId))
                .orElseGet(() -> {
                    User user = userRepository.findById(UUID.fromString(platformUserId))
                            .orElseThrow(() -> new IllegalArgumentException("User not found for platform user ID: " + platformUserId));
                    PlatformUser newUser = new PlatformUser();
                    newUser.setId(UUID.randomUUID());
                    newUser.setPlatformUserId(platformUserId);
                    newUser.setPlatformId("default-platform"); // Adjust as needed
                    newUser.setUser(user);
                    newUser.setBalanceFiat(BigDecimal.ZERO);
                    newUser.setBalanceSui(BigDecimal.ZERO);
                    return platformUserRepository.save(newUser);
                });
    }

    private Wallet getCentralSuiWallet() {
        return walletRepository.findByWalletAddress(CENTRAL_SUI_WALLET_ADDRESS)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setWalletAddress(CENTRAL_SUI_WALLET_ADDRESS);
                    wallet.setBalance(BigDecimal.ZERO);
                    wallet.setCurrencyType(WalletCurrency.SUI);
                    return walletRepository.save(wallet);
                });
    }

    private PlatformUser getCentralPool() {
        return platformUserRepository.findByIdWithPessimisticLock(CENTRAL_POOL_ID)
                .orElseGet(() -> {
                    PlatformUser pool = new PlatformUser();
                    pool.setId(CENTRAL_POOL_ID);
                    pool.setPlatformUserId("central-pool");
                    pool.setPlatformId("default-platform"); // Adjust as needed
                    pool.setBalanceFiat(BigDecimal.ZERO);
                    return platformUserRepository.save(pool);
                });
    }

    private Transaction buildTransaction(LedgerEntry ledger, SuiResponse suiResponse, TransactionType type, UUID platformUserId) {
        return Transaction.builder()
                .id(UUID.fromString(ledger.getReference() != null ? ledger.getReference() : UUID.randomUUID().toString()))
                .senderId(ledger.getSenderId() != null ? ledger.getSenderId() : UUID.randomUUID().toString())
                .receiverId(ledger.getReceiverId() != null ? ledger.getReceiverId() : platformUserId.toString())
                .platformId(ledger.getCompanyId() != null ? ledger.getCompanyId() : UUID.randomUUID().toString())
                .transactionType(type)
                .currency(WalletCurrency.valueOf(ledger.getCurrency() != null ? ledger.getCurrency() : "SUI"))
                .amount(ledger.getAmount() != null ? ledger.getAmount() : BigDecimal.ZERO)
                .status(TransactionStatus.SUCCESSFUL)
                .timestamp(Instant.now())
                .gasFee(suiResponse != null ? suiResponse.getGasFee() : null)
                .onChain(suiResponse != null)
                .build();
    }
}