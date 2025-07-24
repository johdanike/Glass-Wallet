package com.glasswallet.Ledger.service.implementation;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.data.repositories.LedgerRepo;
import com.glasswallet.Ledger.dtos.responses.SuiResponse;
import com.glasswallet.Ledger.enums.LedgerType;
import com.glasswallet.Ledger.exceptions.TypeNotFoundException;
import com.glasswallet.Ledger.service.interfaces.LedgerOrchestrator;
import com.glasswallet.Ledger.service.interfaces.MoveServiceClient;
import com.glasswallet.transaction.data.models.Transaction;
import com.glasswallet.transaction.data.repositories.TransactionRepository;
import com.glasswallet.transaction.enums.TransactionStatus;
import com.glasswallet.transaction.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerOrchestratorService implements LedgerOrchestrator {

    private final LedgerRepo ledgerRepo;
    private final TransactionRepository transactionRepository;
    private final MoveServiceClient moveServiceClient;


    @Override
    public void recordLedgerAndTransaction(LedgerEntry entry) {
        ledgerRepo.save(entry);

        SuiResponse response = moveServiceClient.logOnChain(entry);

        String direction = determineDirection(entry);

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

    private static String determineDirection(LedgerEntry entry) {
        String direction = switch (entry.getType()){
            case CRYPTO_TRANSFER_IN, TRANSFER_IN -> "IN";
            case CRYPTO_TRANSFER_OUT, TRANSFER_OUT -> "OUT";
            default -> "INTERNAL";
        };
        return direction;
    }

    private TransactionType mapLedgerType(LedgerType type) {
        try {
            return type.toTransactionType();
        } catch (Exception e) {
            log.error("Failed to map LedgerType: {}", type, e);
            throw new TypeNotFoundException("Type not found");
        }
    }
}
