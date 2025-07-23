package com.glasswallet.transaction.controller;

import com.glasswallet.transaction.data.models.Transaction;
import com.glasswallet.transaction.services.interfaces.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable String userId) {
        return ResponseEntity.ok(transactionService.getAllTransactionsForUser(userId));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<Transaction>> getCompanyTransactions(@PathVariable String companyId) {
        return ResponseEntity.ok(transactionService.getTransactionsByCompany(companyId));
    }

    @GetMapping("/{txId}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable UUID txId) {
        return transactionService.getTransactionById(txId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
