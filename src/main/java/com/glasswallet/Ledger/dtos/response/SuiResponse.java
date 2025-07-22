package com.glasswallet.Ledger.dtos.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SuiResponse {
    private String txHash;
    private BigDecimal gasFee;
}
