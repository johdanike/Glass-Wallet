package com.glasswallet.transaction.dtos.response;

import com.glasswallet.transaction.data.models.Transaction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
//@AllArgsConstructor
public class TransferResponse {
    private String message;
    private List<UUID> transaction;
}
