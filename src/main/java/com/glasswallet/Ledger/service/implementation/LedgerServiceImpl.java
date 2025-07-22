package com.glasswallet.Ledger.service.implementation;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.data.repositories.LedgerRepo;
import com.glasswallet.Ledger.dtos.request.BulkDisbursementRequest;
import com.glasswallet.Ledger.dtos.request.DepositRequest;
import com.glasswallet.Ledger.dtos.request.TransferRequest;
import com.glasswallet.Ledger.dtos.request.WithdrawalRequest;
import com.glasswallet.Ledger.dtos.response.BulkDisbursementResponse;
import com.glasswallet.Ledger.dtos.response.DepositResponse;
import com.glasswallet.Ledger.dtos.response.TransferResponse;
import com.glasswallet.Ledger.dtos.response.WithdrawalResponse;
import com.glasswallet.Ledger.enums.LedgerType;
import com.glasswallet.Ledger.enums.Status;
import com.glasswallet.Ledger.service.interfaces.LedgerOrchestrator;
import com.glasswallet.Ledger.service.interfaces.LedgerService;
import com.glasswallet.transaction.data.models.Transaction;
import com.glasswallet.transaction.data.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class LedgerServiceImpl implements LedgerService {

    private final LedgerRepo ledgerRepo;
    private final TransactionRepository transactionRepository;
    private final LedgerOrchestrator ledgerOrchestrator;


    @Override
    public DepositResponse recordDeposit(DepositRequest request) {
        LedgerEntry entry = createLedgerEntryFromDeposit(request);
        logTransaction(entry);

        DepositResponse response = new DepositResponse();
        response.setMessage(request.getAmount() + " successfully sent to " + request.getReceiverId());
        return response;
    }


    @Override
    public WithdrawalResponse recordWithdrawal(WithdrawalRequest request) {
        LedgerEntry entry = createLedgerEntryFromWithdrawal(request);
        logTransaction(entry);

        createLedgerEntryFromWithdrawal(request);
        WithdrawalResponse response = new WithdrawalResponse();
        response.setMessage( request.getAmount() + "withdrawn from" + request.getSenderId() +"to" + request.getReceiverId());
        return response;

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

    @Override
    public TransferResponse recordTransfer(TransferRequest request) {
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

//        logTransaction(List.of(sendEntry, receiverEntry));
        TransferResponse response = new TransferResponse();
        response.setMessage(request.getAmount() +
                " transferred from " + request.getSenderId() +
                " to " + request.getReceiverId() +
                " with reference " + request.getReference());

        return response;

    }

    @Override
    public BulkDisbursementResponse recordBulkDisbursement(BulkDisbursementRequest request) {
        List<TransferResponse> results = new ArrayList<>();

        for (TransferRequest transfer : request.getDisbursements()) {
            TransferResponse transferResponse = recordTransfer(transfer);
            results.add(transferResponse);
        }

        BulkDisbursementResponse response = new BulkDisbursementResponse();
        response.setTransferResults(results);
        response.setMessage(results.size() + " transfers processed successfully.");

        return response;
    }

    @Override
    public void logTransaction(Transaction tx) {
        transactionRepository.save(tx);
    }

    private void logTransaction(LedgerEntry entry) {
        ledgerRepo.save(entry);
    }

    private void logTransaction(List<LedgerEntry> entries) {
        ledgerRepo.saveAll(entries);
    }

    private LedgerEntry createLedgerEntryFromDeposit(DepositRequest request) {
        return LedgerEntry.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .companyId(request.getCompanyId())
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .amount(request.getAmount())
                .reference(request.getReference())
                .type(LedgerType.DEPOSIT)
                .status(Status.SUCCESSFUL)
                .currency(request.getCurrency())
                .timestamp(Instant.now())
                .build();
    }


}
