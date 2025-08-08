package com.glasswallet.fiat.service.interfaces;

import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.transaction.data.models.Transaction;
import com.glasswallet.transaction.dtos.request.BulkDisbursementRequest;
import com.glasswallet.transaction.dtos.request.DepositRequest;
import com.glasswallet.transaction.dtos.request.TransferRequest;
import com.glasswallet.transaction.dtos.request.WithdrawalRequest;
import com.glasswallet.transaction.dtos.response.BulkDisbursementResponse;
import com.glasswallet.transaction.dtos.response.DepositResponse;
import com.glasswallet.transaction.dtos.response.TransferResponse;
import com.glasswallet.transaction.dtos.response.WithdrawalResponse;
import com.glasswallet.transaction.enums.TransactionType;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocalCurrencyService {
    DepositResponse processDepositForFiats(DepositRequest request);

    WithdrawalResponse processWithdrawalForFiat(WithdrawalRequest request);
    TransferResponse processTransferForFiats(TransferRequest request);
    BulkDisbursementResponse processBulkDisbursement(BulkDisbursementRequest request);
    List<Transaction> getAllTransactionsForUser(String userId);
    List<Transaction> getTransactionsByCompany(String companyId);
    Optional<Transaction> getTransactionId(UUID txId);

    @Transactional
    Transaction transact(UUID senderId, UUID receiverId, UUID companyId, TransactionType type, WalletCurrency currency, String reference, BigDecimal amount);


}
