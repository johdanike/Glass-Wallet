package com.glasswallet.transaction.dtos.request;

import com.glasswallet.user.data.models.User;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter
public class TransferRequest {
    private User userId;
    private String senderId;
    private String receiverId;
    private BigDecimal amount;
    private String currency;
    private String reference;
    private String companyId;
    private boolean isCrypto;
}
