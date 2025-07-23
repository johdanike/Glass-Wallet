package com.glasswallet.transaction.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class LedgerResponse {
    private String status;
    private String message;
    private String senderId;
    private String receiverId;
    private BigDecimal amount;
    private String currency;
    private String referenceId;
    private String transactionType;
    private Instant timestamp;
    private String transactionId;
    private String companyId;
}
