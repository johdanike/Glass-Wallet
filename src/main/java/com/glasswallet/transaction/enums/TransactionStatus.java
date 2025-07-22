package com.glasswallet.transaction.enums;

public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REFUNDED,
    EXPIRED, SUCCESSFUL;

    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == REFUNDED || this == EXPIRED;
    }
}
