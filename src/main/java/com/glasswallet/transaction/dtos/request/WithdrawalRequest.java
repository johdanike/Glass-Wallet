package com.glasswallet.transaction.dtos.request;

import com.glasswallet.user.data.models.User;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@Builder
public class WithdrawalRequest {
    private User userId;
    private String senderId;
    private BigDecimal amount;
    private String currency;
    private String receiverId;
    private String companyId;
    private String reference;
}
