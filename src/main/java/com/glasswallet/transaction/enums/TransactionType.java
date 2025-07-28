package com.glasswallet.transaction.enums;

import lombok.Getter;

@Getter
public enum TransactionType {
    DEPOSIT("deposit"),
    WITHDRAWAL("withdrawal"),
//    TRANSFER("transfer"),
    FUNDING("funding"),
    FIAT_TRANSFER("fiat_transfer"),
    CRYPTO_TRANSFER("crypto_transfer"),
    BULK_DISBURSEMENT("bulk_disbursement"), EXTERNAL_DEPOSIT("external_deposit"), EXTERNAL_WITHDRAWAL("external_withdrawal");


    private final String type;

    TransactionType(String type) {
        this.type = type;
    }
}
