package com.glasswallet.transaction.dtos.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class WithdrawalResponse {
    private String message;
    private List<UUID> transactionId;
    private String transactionIdOnChain;
    private String status;
    private String platformId;
    private String platformUserId;
}
