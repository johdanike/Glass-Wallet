package com.glasswallet.transaction.enums;

public enum CurrencyType {
    NGN("NGN"),
    SUI("SUI");

    private final String code;

    CurrencyType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
