package com.glasswallet.Ledger.data.repositories;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.user.data.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerRepo extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByUserId(User user);

    @Query("SELECT l FROM LedgerEntry l WHERE l.reference = :ref")
    Optional<LedgerEntry> findByReference(@Param("ref") String reference);

}

