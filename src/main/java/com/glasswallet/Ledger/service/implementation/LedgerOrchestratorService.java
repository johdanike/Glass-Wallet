package com.glasswallet.Ledger.service.implementation;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.data.repositories.LedgerRepo;
import com.glasswallet.Ledger.dtos.response.SuiResponse;
import com.glasswallet.Ledger.enums.LedgerType;
import com.glasswallet.Ledger.service.interfaces.LedgerOrchestrator;
import com.glasswallet.Ledger.service.interfaces.MoveServiceClient;
import com.glasswallet.transaction.data.models.Transaction;
import com.glasswallet.transaction.data.repositories.TransactionRepository;
import com.glasswallet.transaction.enums.TransactionStatus;
import com.glasswallet.transaction.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LedgerOrchestratorService implements LedgerOrchestrator {

    private final LedgerRepo ledgerRepo;
    private final TransactionRepository transactionRepository;
    private final MoveServiceClient moveServiceClient;


    @Override
    public void recordLedgerAndTransaction(LedgerEntry entry) {
        ledgerRepo.save(entry);

        // Call Node.js service
        SuiResponse response = moveServiceClient.logOnChain(entry); // this hits Node.js

        String direction = switch (entry.getType()){
            case CRYPTO_TRANSFER_IN, TRANSFER_IN -> "IN";
            case CRYPTO_TRANSFER_OUT, TRANSFER_OUT -> "OUT";
            default -> null;
        };

        // Save Transaction with Sui metadata
        Transaction tx = Transaction.builder()
                .senderId(entry.getSenderId())
                .receiverId(entry.getReceiverId())
                .platformId(entry.getCompanyId())
                .transactionType(mapLedgerType(entry.getType()))
                .amount(entry.getAmount())
                .direction(direction)
                .status(TransactionStatus.SUCCESSFUL)
                .timestamp(Instant.now())
                .onChain(true)
                .suiTxHash(response.getTxHash())
                .gasFee(response.getGasFee())
                .build();

        transactionRepository.save(tx);
    }


    private TransactionType mapLedgerType(LedgerType type) {
//        try {
//            return TransactionType.valueOf(type.name());
//        } catch (IllegalArgumentException e) {
//            throw new RuntimeException("No matching TransactionType for LedgerType: " + type);
//        }
        return type.toTransactionType();
    }

}
