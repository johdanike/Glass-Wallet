package com.glasswallet.Ledger.service.interfaces;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.dtos.requests.LogTransactionRequest;
import com.glasswallet.Ledger.dtos.responses.SuiResponse;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.transaction.dtos.request.*;
import com.glasswallet.transaction.enums.TransactionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerService {

    LedgerEntry logDeposit(DepositRequest request);

    LedgerEntry logWithdrawal(WithdrawalRequest request);

    List<LedgerEntry> logTransfer(TransferRequest request);

    List<LedgerEntry> logBulkDisbursement(BulkDisbursementRequest request);

    LedgerEntry logTransaction(LogTransactionRequest logTransactionRequest);

    LedgerEntry logExternalTransfer(ExternalTransferRequest request);
}
