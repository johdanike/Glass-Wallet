package com.glasswallet.Ledger.data.repositories;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.dtos.response.LedgerResponse;
import com.glasswallet.Ledger.enums.LedgerType;
import com.glasswallet.Wallet.data.model.Wallet;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerRepo extends CrudRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByWallet(Wallet wallet);


    List<LedgerEntry> findByType(LedgerType type);

    LedgerResponse logTransaction(LedgerEntry entry);
    List<LedgerEntry> findByReferenceId(String referenceId);
}
