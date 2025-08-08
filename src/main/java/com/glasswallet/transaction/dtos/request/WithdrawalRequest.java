package com.glasswallet.transaction.dtos.request;

import com.glasswallet.user.data.models.User;
import lombok.*;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequest {
    private String senderId;
    private BigDecimal amount;
    private String currency;
    private String receiverId;
    private String companyId;
    private String reference;
    private String externalWalletAddress;

}
