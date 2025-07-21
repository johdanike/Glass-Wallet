package com.glasswallet.transaction.data.repositories;

import com.glasswallet.transaction.data.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
}
