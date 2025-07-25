package com.glasswallet.Ledger.dtos.requests;

import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.transaction.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogTransactionRequest {
    private UUID companyId;
    private UUID senderId;
    private UUID receiverId;
    private TransactionType type;
    private BigDecimal amount;
    private WalletCurrency currency;
    private String referenceId;
}
