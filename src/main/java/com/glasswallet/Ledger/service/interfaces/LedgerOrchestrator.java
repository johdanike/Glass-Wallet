package com.glasswallet.Ledger.service.interfaces;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface LedgerOrchestrator {
    void recordLedgerAndTransaction(LedgerEntry entry);
//    void recordBulkLedgerAndTransactions(List<LedgerEntry> entries);

}
