package com.glasswallet.Ledger.service.implementation;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.data.repositories.LedgerRepo;
import com.glasswallet.Ledger.dtos.requests.LogTransactionRequest;
import com.glasswallet.Ledger.enums.LedgerType;
import com.glasswallet.Ledger.enums.Status;
import com.glasswallet.Ledger.service.interfaces.LedgerOrchestrator;
import com.glasswallet.Ledger.service.interfaces.LedgerService;
import com.glasswallet.transaction.data.repositories.TransactionRepository;
import com.glasswallet.transaction.dtos.request.BulkDisbursementRequest;
import com.glasswallet.transaction.dtos.request.DepositRequest;
import com.glasswallet.transaction.dtos.request.TransferRequest;
import com.glasswallet.transaction.dtos.request.WithdrawalRequest;
import com.glasswallet.user.data.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor

public class LedgerServiceImpl implements LedgerService {

    private static final Logger log = LogManager.getLogger(LedgerServiceImpl.class);
    private final LedgerRepo ledgerRepo;
    private final TransactionRepository transactionRepository;
    private final LedgerOrchestrator ledgerOrchestrator;
    private final UserRepository userRepository;

    @Override
    public LedgerEntry logDeposit(DepositRequest request) {
        LedgerEntry entry = createLedgerEntryFromDeposit(request);
        ledgerOrchestrator.recordLedgerAndTransaction(entry);
        logTransaction(entry);
        return ledgerRepo.save(entry);
    }

    @Override
    public LedgerEntry logWithdrawal(WithdrawalRequest request) {
        LedgerEntry entry = createLedgerEntryFromWithdrawal(request);
        ledgerOrchestrator.recordLedgerAndTransaction(entry);
        logTransaction(entry);
        return entry;
    }

    @Override
    public List<LedgerEntry> logTransfer(TransferRequest request) {
        LedgerType outType = request.isCrypto() ? LedgerType.CRYPTO_TRANSFER_OUT : LedgerType.TRANSFER_OUT;
        LedgerType inType = request.isCrypto() ? LedgerType.CRYPTO_TRANSFER_IN : LedgerType.TRANSFER_IN;

        LedgerEntry sendEntry = LedgerEntry.builder()
                .userId(request.getUserId())
                .companyId( request.getCompanyId())
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
                .companyId( request.getCompanyId())
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
        return List.of();
    }

    @Override
    public LedgerEntry logTransaction(LogTransactionRequest logTransactionRequest) {
        return null;
    }

    private LedgerEntry createLedgerEntryFromDeposit(DepositRequest request) {
        return LedgerEntry.builder()
                .companyId(String.valueOf(request.getCompanyId()))
                .senderId(String.valueOf(request.getSenderId()))
                .receiverId(String.valueOf(request.getReceiverId()))
                .amount(request.getAmount())
                .reference(request.getReference())
                .type(LedgerType.DEPOSIT)
                .status(Status.SUCCESSFUL)
                .currency(String.valueOf(request.getCurrency()))
                .timestamp(Instant.now())
                .build();
    }

    private LedgerEntry createLedgerEntryFromWithdrawal(WithdrawalRequest request) {
        return LedgerEntry.builder()
                .senderId( request.getSenderId()  )
                .companyId( request.getCompanyId() )
                .userId( request.getUserId() )
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .receiverId( request.getReceiverId() )
                .reference(request.getReference())
                .type(LedgerType.WITHDRAWAL)
                .status( Status.PENDING )
                .timestamp( Instant.now() )
                .build();
    }

    private void logTransaction(LedgerEntry entry) {
        ledgerRepo.save(entry);
    }

}
