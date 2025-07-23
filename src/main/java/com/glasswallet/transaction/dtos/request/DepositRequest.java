package com.glasswallet.transaction.dtos.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.glasswallet.Ledger.utils.UserDeserializer;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.user.data.models.User;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Getter
@Setter
@AllArgsConstructor
@Builder
public class DepositRequest {
    @JsonDeserialize(using = UserDeserializer.class)
    private UUID senderId;
    private UUID receiverId;
    private UUID companyId;
    private WalletCurrency currency;
    private BigDecimal amount;
    private String reference;

}
