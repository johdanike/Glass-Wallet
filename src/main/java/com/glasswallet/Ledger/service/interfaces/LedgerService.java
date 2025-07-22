package com.glasswallet.Ledger.service.interfaces;

import com.glasswallet.Ledger.dtos.request.BulkDisbursementRequest;
import com.glasswallet.Ledger.dtos.request.DepositRequest;
import com.glasswallet.Ledger.dtos.request.TransferRequest;
import com.glasswallet.Ledger.dtos.request.WithdrawalRequest;
import com.glasswallet.Ledger.dtos.response.BulkDisbursementResponse;
import com.glasswallet.Ledger.dtos.response.DepositResponse;
import com.glasswallet.Ledger.dtos.response.TransferResponse;
import com.glasswallet.Ledger.dtos.response.WithdrawalResponse;
import com.glasswallet.transaction.data.models.Transaction;

public interface LedgerService {
    DepositResponse recordDeposit(DepositRequest request);
    WithdrawalResponse recordWithdrawal(WithdrawalRequest request);
    TransferResponse recordTransfer(TransferRequest request);
    BulkDisbursementResponse recordBulkDisbursement(BulkDisbursementRequest request);
    void logTransaction(Transaction tx);
}
