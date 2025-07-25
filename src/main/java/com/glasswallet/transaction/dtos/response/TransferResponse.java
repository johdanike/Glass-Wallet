package com.glasswallet.transaction.dtos.response;

import com.glasswallet.transaction.data.models.Transaction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
//@AllArgsConstructor
public class TransferResponse {
    private String message;
    private Transaction transaction;
}
