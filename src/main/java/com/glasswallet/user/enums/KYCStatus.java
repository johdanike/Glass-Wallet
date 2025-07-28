package com.glasswallet.user.enums;

public enum KYCStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String status;

    KYCStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
