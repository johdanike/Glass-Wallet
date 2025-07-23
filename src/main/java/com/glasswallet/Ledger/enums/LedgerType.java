package com.glasswallet.Ledger.enums;

import com.glasswallet.transaction.enums.TransactionType;

public enum LedgerType {
    BULK_DISBURSEMENT(TransactionType.BULK_DISBURSEMENT),
    DEPOSIT(TransactionType.DEPOSIT),
    WITHDRAWAL(TransactionType.WITHDRAWAL),
    TRANSFER_IN(TransactionType.FIAT_TRANSFER),
    TRANSFER_OUT(TransactionType.FIAT_TRANSFER),
    FUNDING(TransactionType.FUNDING),
    CRYPTO_TRANSFER_IN(TransactionType.CRYPTO_TRANSFER),
    CRYPTO_TRANSFER_OUT(TransactionType.CRYPTO_TRANSFER);

    private final TransactionType mappedTransactionType;

    LedgerType(TransactionType mappedTransactionType) {
        this.mappedTransactionType = mappedTransactionType;
    }

    public TransactionType toTransactionType() {
        return mappedTransactionType;
    }
}
