package com.glasswallet.transaction.dtos.response;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ExternalTransferResponse {
    private String message;
    private List<UUID> transactionId;
    private String platformId;
    private String platformUserId;
    private String transactionIdOnChain;
}