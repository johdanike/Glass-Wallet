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
import com.glasswallet.Ledger.service.interfaces.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class LedgerServiceImpl implements LedgerService {

    @Autowired
    private final LedgerRepo ledgerRepo;



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
        LedgerEntry entry = LedgerEntry.builder()
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
        return entry;
    }

    @Override
    public TransferResponse recordTransfer(TransferRequest request) {
        LedgerEntry sendEntry = LedgerEntry.builder()
                .userId(request.getUserId())
                .senderId(request.getSenderId())
                .type(LedgerType.TRANSFER_OUT)
                .status(Status.SUCCESSFUL)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .reference(request.getReference())
                .timestamp(Instant.now())
                .build();

        LedgerEntry receiverEntry = LedgerEntry.builder()
                .userId(request.getUserId())
                .receiverId(request.getReceiverId())
                .type(LedgerType.TRANSFER_IN)
                .status(Status.SUCCESSFUL)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .reference(request.getReference())
                .timestamp(Instant.now())
                .build();

        logTransaction(List.of(sendEntry, receiverEntry));
        TransferResponse response = new TransferResponse();
        response.setMessage(request.getAmount() +
                " transferred from " + request.getSenderId() +
                " to " + request.getReceiverId() +
                " with reference " + request.getReference());

        return response;

    }

    @Override
    public BulkDisbursementResponse recordBulkDisbursement(BulkDisbursementRequest request) {
        for (TransferRequest transfer : request.getDisbursements()) {
            recordTransfer( transfer );

            BulkDisbursementResponse response = new BulkDisbursementResponse();
            response.setMessage( request.getAmount() +
                    " withdrawn from " + request.getSenderId() + " to " + request.getReceiverId() + " with a reference of " + request.getReference() + " was successful." );

            return response;
        }
        return null;
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
