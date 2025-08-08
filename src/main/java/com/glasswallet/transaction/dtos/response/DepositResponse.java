package com.glasswallet.transaction.dtos.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class DepositResponse {
    private String message;
    private List<UUID> transactionId;


}
