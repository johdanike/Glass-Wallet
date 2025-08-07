package com.glasswallet.fiat.service.implementation;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.dtos.responses.SuiResponse;
import com.glasswallet.Ledger.service.interfaces.LedgerService;
import com.glasswallet.Ledger.service.interfaces.MoveServiceClient;
import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.data.repositories.WalletRepository;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.Wallet.exceptions.InsufficientBalanceException;
import com.glasswallet.fiat.service.interfaces.LocalCurrencyService;
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
import com.glasswallet.transaction.services.implementations.TransactionServiceImpl;
import com.glasswallet.transaction.services.interfaces.TransactionService;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.glasswallet.Ledger.service.implementation.LedgerServiceImpl.*;


@Service
@RequiredArgsConstructor
public class LocalCurrencyImpl implements LocalCurrencyService {


    @Value("${central.pool.id:00000000-0000-0000-0000-000000000001}")
    private UUID centralPoolId;

    @Value("${central.sui.wallet.address:0x9616a7936669d6276a06fa72edd30d95ae9d67d973ee47d856a830aed06096ba}")
    private String centralSuiWalletAddress;

    @Value("${wallet.withdraw.service.on.sui:https://glass-wallet-listener.onrender.com/api/withdrawSuiCoin}")
    private String withdrawSuiEndpoint;

    private final LedgerService ledgerService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PlatformUserRepository platformUserRepository;
    private final WalletRepository walletRepository;
    private final RestTemplate restTemplate;
    private final TransactionServiceImpl transactionServiceImpl;
    private final MoveServiceClient moveServiceClient;
    private static final Logger log = LoggerFactory.getLogger( LedgerEntry.class);

    @Override
    public DepositResponse processDepositForFiats(DepositRequest request) {
        return executeDeposit(request, WalletCurrency.NGN);

    }
    private DepositResponse executeDeposit(DepositRequest request, WalletCurrency expectedCurrency) {
        if (request == null || request.getReceiverId() == null) {
            throw new IllegalArgumentException("Deposit request or receiver ID cannot be null");
        }

        checkAmountPositive(request.getAmount());

        WalletCurrency currency = WalletCurrency.valueOf(request.getCurrency().name());
        if (currency != expectedCurrency) {
            throw new IllegalArgumentException("Invalid currency. Expected: " + expectedCurrency + ", but got: " + currency);
        }

        PlatformUser user = getPlatformUserWithLock(request.getReceiverId().toString());
        if (user == null) {
            throw new IllegalArgumentException("Receiver not found: " + request.getReceiverId());
        }

        // Balance update
        if (currency == WalletCurrency.SUI) {
            Wallet centralWallet = getCentralSuiWallet();
            centralWallet.setBalance(centralWallet.getBalance().add(request.getAmount()));
            user.setBalanceSui(user.getBalanceSui().add(request.getAmount()));
            walletRepository.save(centralWallet);
        } else if (currency == WalletCurrency.NGN) {
            PlatformUser centralPool = getCentralPool();
            centralPool.setBalanceFiat(centralPool.getBalanceFiat().add(request.getAmount()));
            user.setBalanceFiat(user.getBalanceFiat().add(request.getAmount()));
            platformUserRepository.save(centralPool);
        } else {
            throw new UnsupportedOperationException("Unsupported currency: " + currency);
        }

        // Ledger and transaction
        LedgerEntry ledger = ledgerService.logDeposit(request);
        List<Transaction> transactions = logOnChainAndSaveTransaction(
                TransactionType.DEPOSIT,
                List.of(ledger),
                request.getReceiverId(),
                null,
                null
        );
        transactionRepository.saveAll(transactions);

        DepositResponse response = new DepositResponse();
        response.setMessage("Deposit successful.");
        response.setTransactionId(transactions.stream().map(Transaction::getId).collect(Collectors.toList()));
        return response;
    }

    @Override
    public WithdrawalResponse processWithdrawalForFiat(WithdrawalRequest request) {
        return executeWithdrawal(request, WalletCurrency.NGN);


    }
    private WithdrawalResponse executeWithdrawal(WithdrawalRequest request, WalletCurrency expectedCurrency) {
        if (request == null || request.getSenderId() == null) {
            throw new IllegalArgumentException("Withdrawal request or sender ID cannot be null");
        }

        checkAmountPositive(request.getAmount());

        WalletCurrency currency = WalletCurrency.valueOf(request.getCurrency());
        if (currency != expectedCurrency) {
            throw new IllegalArgumentException("Invalid currency. Expected: " + expectedCurrency + ", but got: " + currency);
        }

        PlatformUser user = getPlatformUserWithLock(request.getSenderId());
        if (user == null) {
            throw new IllegalArgumentException("Sender not found: " + request.getSenderId());
        }

        if (currency == WalletCurrency.SUI) {
            Wallet centralWallet = getCentralSuiWallet();
            validateBalance(centralWallet.getBalance(), request.getAmount(), "Insufficient SUI balance in central wallet.");
            validateBalance(user.getBalanceSui(), request.getAmount(), "Insufficient SUI balance for user.");

            centralWallet.setBalance(centralWallet.getBalance().subtract(request.getAmount()));
            user.setBalanceSui(user.getBalanceSui().subtract(request.getAmount()));
            walletRepository.save(centralWallet);
        } else if (currency == WalletCurrency.NGN) {
            PlatformUser centralPool = getCentralPool();
            validateBalance(centralPool.getBalanceFiat(), request.getAmount(), "Insufficient fiat balance in central pool.");
            validateBalance(user.getBalanceFiat(), request.getAmount(), "Insufficient fiat balance for user.");

            centralPool.setBalanceFiat(centralPool.getBalanceFiat().subtract(request.getAmount()));
            user.setBalanceFiat(user.getBalanceFiat().subtract(request.getAmount()));
            platformUserRepository.save(centralPool);
        } else {
            throw new UnsupportedOperationException("Unsupported currency: " + currency);
        }

        // Call Node.js endpoint for on-chain withdrawal
        WithdrawalRequest payload = new WithdrawalRequest();
        payload.setSenderId(user.getPlatformUserId());
        payload.setAmount(request.getAmount());
        payload.setExternalWalletAddress(request.getExternalWalletAddress());

        ResponseEntity<WithdrawalResponse> response = restTemplate.postForEntity(
                withdrawSuiEndpoint,
                payload,
                WithdrawalResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || !"ok".equals(response.getBody().getStatus())) {
            throw new RuntimeException("Failed to process withdrawal on chain: " +
                    (response.getBody() != null ? response.getBody().getMessage() : "Unknown error"));
        }

        String transactionIdOnChain = response.getBody().getTransactionId().toString();
        LedgerEntry ledger = ledgerService.logWithdrawal(request, transactionIdOnChain, user.getPlatformId(), user.getPlatformUserId());

        List<Transaction> transactions = logOnChainAndSaveTransaction(
                TransactionType.WITHDRAWAL,
                List.of(ledger),
                user.getId(),
                null,
                request.getExternalWalletAddress()
        );
        transactionRepository.saveAll(transactions);

        WithdrawalResponse withdrawalResponse = new WithdrawalResponse();
        withdrawalResponse.setMessage("Withdrawal successful.");
        withdrawalResponse.setTransactionId(transactions.stream().map(Transaction::getId).collect(Collectors.toList()));
        withdrawalResponse.setTransactionIdOnChain(transactionIdOnChain);
        withdrawalResponse.setStatus(response.getBody().getStatus());
        withdrawalResponse.setPlatformId(user.getPlatformId());
        withdrawalResponse.setPlatformUserId(user.getPlatformUserId());
        return withdrawalResponse;
    }

    @Override
    public TransferResponse processTransferForFiats(TransferRequest request) {
        return executeTransfer(request, WalletCurrency.NGN, TransactionType.FIAT_TRANSFER);
    }
    @Override
    public BulkDisbursementResponse processBulkDisbursement(BulkDisbursementRequest request) {

        return executeProcessBulkDisbursement(request) ;
    }


  private BulkDisbursementResponse executeProcessBulkDisbursement(BulkDisbursementRequest request) {
        // Validate request
        if (request == null || request.getSenderId() == null || request.getDisbursements() == null || request.getDisbursements().isEmpty()) {
            throw new IllegalArgumentException("Bulk disbursement request, sender ID, or disbursements cannot be null or empty");
        }

        BigDecimal totalAmount = request.getTotalAmount();
        checkAmountPositive(totalAmount);

        BigDecimal sumOfDisbursements = request.getDisbursements().stream()
                .map(TransferRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(sumOfDisbursements) != 0) {
            throw new IllegalArgumentException("Total amount must equal the sum of individual disbursement amounts");
        }

        PlatformUser sender = getPlatformUserWithLock(request.getSenderId());
        if (sender == null) {
            throw new IllegalArgumentException("Sender not found: " + request.getSenderId());
        }


        BigDecimal totalFiat = BigDecimal.ZERO;
        for (TransferRequest disbursement : request.getDisbursements()) {
            WalletCurrency currency = WalletCurrency.valueOf(disbursement.getCurrency());
            if (currency != WalletCurrency.NGN) {
                throw new IllegalArgumentException("Only fiat (NGN) disbursements are supported in this method.");
            }
            totalFiat = totalFiat.add(disbursement.getAmount());
        }


        validateBalance(sender.getBalanceFiat(), totalFiat, "Insufficient fiat balance for sender.");
        sender.setBalanceFiat(sender.getBalanceFiat().subtract(totalFiat));
        platformUserRepository.save(sender);

        // Log ledger entries
        UUID companyId = UUID.fromString(request.getCompanyId());
        List<LedgerEntry> ledgerEntries = ledgerService.logBulkDisbursement(request);
        if (ledgerEntries.isEmpty()) {
            throw new IllegalStateException("Failed to log bulk disbursement entries");
        }


        for (TransferRequest disbursement : request.getDisbursements()) {
            UUID receiverId = UUID.fromString(disbursement.getReceiverId());
            WalletCurrency currency = WalletCurrency.valueOf(disbursement.getCurrency());


            transactForBulk(sender, receiverId, companyId, TransactionType.FIAT_TRANSFER, currency,
                    disbursement.getReference(), disbursement.getAmount());
        }


        List<Transaction> transactions = new ArrayList<>();
        for (LedgerEntry entry : ledgerEntries) {
            WalletCurrency currency = WalletCurrency.valueOf(entry.getCurrency());
            if (currency != WalletCurrency.NGN) {
                throw new IllegalStateException("Unexpected non-fiat ledger entry in fiat disbursement flow");
            }

            List<Transaction> entryTransactions = logOnChainAndSaveTransaction(
                    TransactionType.FIAT_TRANSFER,
                    List.of(entry),
                    sender.getId(),
                    null,
                    null
            );
            transactions.addAll(entryTransactions);
        }

        transactionRepository.saveAll(transactions);


        for (LedgerEntry entry : ledgerEntries) {
            WalletCurrency currency = WalletCurrency.valueOf(entry.getCurrency());
            if (currency == WalletCurrency.NGN) {
                PlatformUser centralPool = getCentralPool();
                centralPool.setBalanceFiat(centralPool.getBalanceFiat().add(entry.getAmount()));
                platformUserRepository.save(centralPool);
            }
        }

        // Build and return response
        BulkDisbursementResponse response = new BulkDisbursementResponse();
        response.setMessage("Bulk disbursement successful.");
        response.setTransferResults(transactions.stream().map(Transaction::getId).collect(Collectors.toList()));
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
    public Optional<Transaction> getTransactionId(UUID txId) {
        return transactionRepository.findById(txId);
    }

    @Override
    public Transaction transact(UUID senderId, UUID receiverId, UUID companyId, TransactionType type, WalletCurrency currency, String reference, BigDecimal amount) {
        return null;
    }


    private void checkAmountPositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
    }
    private PlatformUser getPlatformUserWithLock(String platformUserId) {
        if (platformUserId == null) {
            throw new IllegalArgumentException("Platform user ID cannot be null");
        }
        return platformUserRepository.findByPlatformUserId(platformUserId)
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
    private List<Transaction> logOnChainAndSaveTransaction(TransactionType type, List<LedgerEntry> ledgers, UUID senderId, UUID receiverId, String externalWalletAddress) {
        return ledgers.stream().map(ledger -> {
            UUID platformUserId = ledger.getSenderId() != null ? UUID.fromString(ledger.getSenderId()) :
                    ledger.getReceiverId() != null ? UUID.fromString(ledger.getReceiverId()) : senderId;
            SuiResponse suiResponse = null;
            try {
                if (type == TransactionType.WITHDRAWAL || type == TransactionType.EXTERNAL_WITHDRAWAL) {

                } else {
                    suiResponse = moveServiceClient.logOnChain(ledger);
                }
            } catch (Exception e) {
                log.warn("Failed to log transaction on-chain: {}", e.getMessage());
            }

            return Transaction.builder()
                    .id(UUID.fromString(ledger.getReference() != null ? ledger.getReference() : UUID.randomUUID().toString()))
                    .senderId(ledger.getSenderId() != null ? ledger.getSenderId() : senderId.toString())
                    .receiverId(ledger.getReceiverId() != null ? ledger.getReceiverId() : receiverId != null ? receiverId.toString() : externalWalletAddress)
                    .platformId(ledger.getCompanyId() != null ? ledger.getCompanyId() : UUID.randomUUID().toString())
                    .transactionType(type)
                    .amount(ledger.getAmount() != null ? ledger.getAmount() : BigDecimal.ZERO)
                    .currency(WalletCurrency.valueOf(ledger.getCurrency() != null ? ledger.getCurrency() : "SUI"))
                    .status(suiResponse != null ? TransactionStatus.SUCCESSFUL : TransactionStatus.FAILED)
                    .timestamp( Instant.now())
                    .gasFee(suiResponse != null ? suiResponse.getGasFee() : null)
                    .onChain(suiResponse != null || (type == TransactionType.WITHDRAWAL || type == TransactionType.EXTERNAL_WITHDRAWAL))
                    .externalWalletAddress(externalWalletAddress)
                    .build();
        }).map(transactionRepository::save).collect( Collectors.toList());
    }
    private void validateBalance(BigDecimal balance, BigDecimal amount, String errorMessage) {
        if (balance == null || amount == null || balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(errorMessage);
        }
    }

    private void checkTransactionAndCurrencyType(WalletCurrency currency, TransactionType type) {

        if (type == TransactionType.FIAT_TRANSFER && currency != WalletCurrency.NGN) {
            throw new IllegalArgumentException("FIAT_TRANSFER must use NGN wallet");
        }
    }


    private Wallet getCentralSuiWallet() {
        return walletRepository.findByWalletAddress(centralSuiWalletAddress)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setWalletAddress(centralSuiWalletAddress);
                    wallet.setBalance(BigDecimal.ZERO);
                    wallet.setCurrencyType(WalletCurrency.SUI);
                    return walletRepository.save(wallet);
                });
    }

    private PlatformUser getCentralPool() {
        return platformUserRepository.findByIdWithPessimisticLock(centralPoolId)
                .orElseGet(() -> {
                    PlatformUser pool = new PlatformUser();
                    pool.setId(centralPoolId);
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
    private TransferResponse executeTransfer(TransferRequest request, WalletCurrency expectedCurrency, TransactionType transactionType) {
        if (request == null || request.getSenderId() == null || request.getReceiverId() == null || request.getCompanyId() == null) {
            throw new IllegalArgumentException("Transfer request or IDs cannot be null");
        }

        checkAmountPositive(request.getAmount());

        WalletCurrency currency = WalletCurrency.valueOf(request.getCurrency());
        if (currency != expectedCurrency) {
            throw new IllegalArgumentException("Invalid currency. Expected: " + expectedCurrency + ", but got: " + currency);
        }

        checkTransactionAndCurrencyType(currency, transactionType);

        UUID senderId = UUID.fromString(request.getSenderId());
        UUID receiverId = UUID.fromString(request.getReceiverId());
        UUID companyId = UUID.fromString(request.getCompanyId());

        PlatformUser receiver = getPlatformUserWithLock(receiverId.toString());
        if (receiver == null) {
            throw new IllegalArgumentException("Receiver not found: " + receiverId);
        }

        transact(senderId, receiverId, companyId, transactionType, currency, request.getReference(), request.getAmount());

        List<LedgerEntry> ledgerEntries = ledgerService.logTransfer(request);
        if (ledgerEntries.isEmpty()) {
            throw new IllegalStateException("Failed to log transfer entries");
        }

        List<Transaction> transactions = logOnChainAndSaveTransaction(
                transactionType,
                ledgerEntries,
                senderId,
                receiverId,
                null
        );
        transactionRepository.saveAll(transactions);

        TransferResponse response = new TransferResponse();
        response.setMessage("Transfer successful.");
        response.setTransaction(transactions.stream().map(Transaction::getId).collect(Collectors.toList()));
        return response;
    }
    @Transactional
    public Transaction transactForBulk(PlatformUser sender, UUID receiverId, UUID companyId, TransactionType type, WalletCurrency currency, String reference, BigDecimal amount) {
        checkAmountPositive(amount);
        checkTransactionAndCurrencyType(currency, type);

        PlatformUser receiver = getPlatformUserWithLock(receiverId.toString());
        if (receiver == null) {
            throw new IllegalArgumentException("Receiver not found: " + receiverId);
        }

        if (currency == WalletCurrency.SUI) {
            receiver.setBalanceSui(receiver.getBalanceSui().add(amount));
        } else {
            receiver.setBalanceFiat(receiver.getBalanceFiat().add(amount));
        }

        platformUserRepository.save(receiver);

        TransferRequest request = TransferRequest.builder()
                .senderId(sender.getId().toString())
                .receiverId(receiverId.toString())
                .companyId(companyId.toString())
                .amount(amount)
                .currency(currency.name())
                .reference(reference)
                .isCrypto(currency == WalletCurrency.NGN)
                .build();
        List<LedgerEntry> ledgerEntries = ledgerService.logTransfer(request);
        if (ledgerEntries.isEmpty()) {
            throw new IllegalStateException("Failed to log transfer entries");
        }

        List<Transaction> transactions = logOnChainAndSaveTransaction(type, ledgerEntries, sender.getId(), receiverId, null);
        transactionRepository.saveAll(transactions);
        return transactions.get(0);
    }
}
