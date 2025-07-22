package com.glasswallet.Ledger.dtos.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkDisbursementResponse {
    private String message;
    private List<TransferResponse> transferResults;

}
