package com.glasswallet.Ledger.service.implementation;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.data.repositories.LedgerRepo;
import com.glasswallet.Ledger.dtos.requests.LogTransactionRequest;
import com.glasswallet.Ledger.enums.LedgerType;
import com.glasswallet.Ledger.enums.Status;
import com.glasswallet.Ledger.service.interfaces.LedgerOrchestrator;
import com.glasswallet.Ledger.service.interfaces.LedgerService;
import com.glasswallet.platform.data.repositories.PlatformUserRepository;
import com.glasswallet.transaction.data.repositories.TransactionRepository;
import com.glasswallet.transaction.dtos.request.BulkDisbursementRequest;
import com.glasswallet.transaction.dtos.request.DepositRequest;
import com.glasswallet.transaction.dtos.request.TransferRequest;
import com.glasswallet.transaction.dtos.request.WithdrawalRequest;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private static final Logger log = LogManager.getLogger(LedgerServiceImpl.class);
    private final LedgerRepo ledgerRepo;
    private final TransactionRepository transactionRepository;
    private final LedgerOrchestrator ledgerOrchestrator;
    private final PlatformUserRepository userRepository;

    @Override
    public LedgerEntry logDeposit(DepositRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0 ||
                request.getCompanyId() == null) {
            log.warn("Invalid deposit request: amount or companyId is invalid");
            return null;
        }

        LedgerEntry entry = createLedgerEntryFromDeposit(request);
        ledgerOrchestrator.recordLedgerAndTransaction(entry);
        logTransaction(entry);
        return ledgerRepo.save(entry);
    }

    @Override
    public LedgerEntry logWithdrawal(WithdrawalRequest request) {
        if (request.getUserId() == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid withdrawal request: userId or amount is invalid");
            return null;
        }

        // Remove balance check, rely on TransactionServiceImpl
        LedgerEntry entry = createLedgerEntryFromWithdrawal(request);
        ledgerOrchestrator.recordLedgerAndTransaction(entry);
        logTransaction(entry);
        return ledgerRepo.save(entry);
    }

    @Override
    public List<LedgerEntry> logTransfer(TransferRequest request) {
        if (request.getSenderId().equals(request.getReceiverId())) {
            log.warn("Transfer aborted: Sender and receiver IDs are the same: {}", request.getSenderId());
            return List.of();
        }

        LedgerType outType = request.isCrypto() ? LedgerType.CRYPTO_TRANSFER_OUT : LedgerType.TRANSFER_OUT;
        LedgerType inType = request.isCrypto() ? LedgerType.CRYPTO_TRANSFER_IN : LedgerType.TRANSFER_IN;

        LedgerEntry sendEntry = LedgerEntry.builder()
                .userId(request.getUserId())
                .companyId(request.getCompanyId())
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .type(outType)
                .status(Status.SUCCESSFUL)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .reference(request.getReference())
                .timestamp(Instant.now())
                .build();

        LedgerEntry receiverEntry = LedgerEntry.builder()
                .userId(request.getUserId())
                .companyId(request.getCompanyId())
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .type(inType)
                .status(Status.SUCCESSFUL)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .reference(request.getReference())
                .timestamp(Instant.now())
                .build();

        ledgerOrchestrator.recordLedgerAndTransaction(sendEntry);
        ledgerOrchestrator.recordLedgerAndTransaction(receiverEntry);

        ledgerRepo.saveAll(List.of(sendEntry, receiverEntry));
        return List.of(sendEntry, receiverEntry);
    }

    @Override
    public List<LedgerEntry> logBulkDisbursement(BulkDisbursementRequest request) {
        List<TransferRequest> disbursements = request.getDisbursements();
        if (disbursements == null) {
            log.warn("Bulk disbursement request has null disbursements");
            return List.of();
        }
        List<LedgerEntry> entries = request.getDisbursements().stream()
                .map(disbursement -> LedgerEntry.builder()
                        .userId(disbursement.getUserId())
                        .companyId(disbursement.getCompanyId())
                        .senderId(disbursement.getSenderId())
                        .receiverId(disbursement.getReceiverId())
                        .type(LedgerType.BULK_DISBURSEMENT)
                        .status(Status.SUCCESSFUL)
                        .amount(disbursement.getAmount())
                        .currency(disbursement.getCurrency())
                        .reference(disbursement.getReference())
                        .timestamp(Instant.now())
                        .build())
                .toList();

        entries.forEach(ledgerOrchestrator::recordLedgerAndTransaction);
        return ledgerRepo.saveAll(entries);
    }

    @Override
    public LedgerEntry logTransaction(LogTransactionRequest logTransactionRequest) {
        if (logTransactionRequest.getAmount() == null || logTransactionRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid transaction request: amount is invalid");
            return null;
        }

        LedgerEntry entry = LedgerEntry.builder()
                .senderId(String.valueOf(logTransactionRequest.getSenderId()))
                .receiverId(String.valueOf(logTransactionRequest.getReceiverId()))
                .companyId(String.valueOf(logTransactionRequest.getCompanyId()))
                .type(LedgerType.valueOf(logTransactionRequest.getType().name())) // Map TransactionType to LedgerType
                .status(Status.SUCCESSFUL)
                .amount(logTransactionRequest.getAmount())
                .currency(logTransactionRequest.getCurrency().name())
                .reference(logTransactionRequest.getReferenceId())
                .timestamp(Instant.now())
                .build();

        ledgerOrchestrator.recordLedgerAndTransaction(entry);
        return ledgerRepo.save(entry);
    }

    private LedgerEntry createLedgerEntryFromDeposit(DepositRequest request) {
        return LedgerEntry.builder()
                .companyId(request.getCompanyId() != null ? request.getCompanyId().toString() : null)
                .senderId(request.getSenderId() != null ? request.getSenderId().toString() : null)
                .receiverId(request.getReceiverId() != null ? request.getReceiverId().toString() : null)
                .amount(request.getAmount())
                .reference(request.getReference())
                .type(LedgerType.DEPOSIT)
                .status(Status.SUCCESSFUL)
                .currency(request.getCurrency() != null ? request.getCurrency().toString() : null)
                .timestamp(Instant.now())
                .build();
    }

    private LedgerEntry createLedgerEntryFromWithdrawal(WithdrawalRequest request) {
        return LedgerEntry.builder()
                .senderId(request.getSenderId())
                .companyId(request.getCompanyId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .receiverId(request.getReceiverId())
                .reference(request.getReference())
                .type(LedgerType.WITHDRAWAL)
                .status(Status.PENDING) // Updated to PENDING until confirmed
                .timestamp(Instant.now())
                .build();
    }

    private void logTransaction(LedgerEntry entry) {
        ledgerRepo.save(entry);
    }


}