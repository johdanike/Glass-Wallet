package com.glasswallet.Wallet.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WalletBalanceResponse {
    private BigDecimal fiatBalance;
    private String fiatCurrency;
    private BigDecimal suiBalance;
    private String suiToken;
    private BigDecimal suiToNgnRate;
    private BigDecimal suiEquivalentOfFiat;
    private BigDecimal fiatEquivalentOfSui;
}
