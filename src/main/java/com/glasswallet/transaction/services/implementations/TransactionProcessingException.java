package com.glasswallet.transaction.services.implementations;

public class TransactionProcessingException extends Throwable {
    public TransactionProcessingException(String blockchainOperationFailed) {
        super(blockchainOperationFailed);
    }
}
