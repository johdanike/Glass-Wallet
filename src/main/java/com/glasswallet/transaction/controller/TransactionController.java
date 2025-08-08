package com.glasswallet.transaction.controller;

import com.glasswallet.transaction.dtos.request.BulkDisbursementRequest;
import com.glasswallet.transaction.dtos.request.DepositRequest;
import com.glasswallet.transaction.dtos.request.TransferRequest;
import com.glasswallet.transaction.dtos.request.WithdrawalRequest;
import com.glasswallet.transaction.dtos.response.BulkDisbursementResponse;
import com.glasswallet.transaction.dtos.response.DepositResponse;
import com.glasswallet.transaction.dtos.response.TransferResponse;
import com.glasswallet.transaction.dtos.response.WithdrawalResponse;
import com.glasswallet.transaction.enums.TransactionType;
import com.glasswallet.transaction.services.interfaces.TransactionService;
import com.glasswallet.transaction.data.models.Transaction;
import com.glasswallet.Wallet.enums.WalletCurrency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/depositSui")
    public ResponseEntity<DepositResponse> deposit(@RequestBody DepositRequest request) {
        return ResponseEntity.ok(transactionService.processDepositForSui(request));
    }

    @PostMapping("/withdrawSui")
    public ResponseEntity<WithdrawalResponse> withdraw(@RequestBody WithdrawalRequest request) {
        return ResponseEntity.ok(transactionService.processWithdrawal(request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
        return ResponseEntity.ok(transactionService.processTransfer(request));
    }

    @PostMapping("/bulk-disburse")
    public ResponseEntity<BulkDisbursementResponse> bulkDisburse(@RequestBody BulkDisbursementRequest request) {
        return ResponseEntity.ok(transactionService.processBulkDisbursement(request));
    }

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
        Optional<Transaction> transaction = transactionService.getTransactionId(txId);
        return transaction.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Caution: Admin/internal only. Normally not public.
    @PostMapping("/transact")
    public ResponseEntity<Transaction> transact(
            @RequestParam UUID senderId,
            @RequestParam UUID receiverId,
            @RequestParam UUID companyId,
            @RequestParam TransactionType type,
            @RequestParam WalletCurrency currency,
            @RequestParam String reference,
            @RequestParam BigDecimal amount
    ) {
        Transaction tx = transactionService.transact(senderId, receiverId, companyId, type, currency, reference, amount);
        return ResponseEntity.ok(tx);
    }
}