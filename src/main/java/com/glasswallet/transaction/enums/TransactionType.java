package com.glasswallet.transaction.enums;

import lombok.Getter;

@Getter
public enum TransactionType {
    DEPOSIT("deposit"),
    WITHDRAWAL("withdrawal"),
    TRANSFER("transfer"),;


    private final String type;

    TransactionType(String type) {
        this.type = type;
    }

}
