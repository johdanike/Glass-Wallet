package com.glasswallet.transaction.dtos.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ExternalTransferRequest {
    private UUID senderId;
    private String externalWalletAddress; // Address of the external wallet (e.g., Slush)
    private BigDecimal amount;
    private String currency; // e.g., "SUI" or "NGN"
    private boolean isInbound; // true for receiving, false for sending
}