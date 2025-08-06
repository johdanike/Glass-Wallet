package com.glasswallet.transaction.data.models;

import com.glasswallet.transaction.enums.TransactionStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class PayStackData {
    private String reference;
    private int amount;
    private TransactionStatus status;
    private String paidAt;
    private String channel;
}
