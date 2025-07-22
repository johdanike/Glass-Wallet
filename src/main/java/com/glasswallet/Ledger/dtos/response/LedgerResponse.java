package com.glasswallet.Ledger.dtos.response;

import lombok.Data;

@Data
public class LedgerResponse {
    private String status;
    private String message;
    private Object data;

}
