package com.glasswallet.transaction.exception;

public class TransactionProcessingException extends Throwable {
    public TransactionProcessingException(String blockchainOperationFailed) {
        super(blockchainOperationFailed);
    }
}
