package com.glasswallet.Ledger.data.repositories;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.enums.LedgerType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerRepo extends CrudRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByWalletId(UUID walletId);


    List<LedgerEntry> findByType(LedgerType type);


    List<LedgerEntry> findByReferenceId(String referenceId);
}
