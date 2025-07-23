package com.glasswallet.Ledger.dtos.responses;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SuiResponse {
    private String txHash;
    private BigDecimal gasFee;
    private boolean status;
    private String message;
}
