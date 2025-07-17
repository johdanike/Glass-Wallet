
package com.glasswallet.Wallet.dtos.response;

import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.Wallet.enums.WalletStatus;
import com.glasswallet.Wallet.enums.WalletType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
public class CreateWalletResponse {
    private String message;
    private WalletStatus walletStatus;
    private String accountNumber;
    private WalletType walletType;
    private WalletCurrency walletCurrency;
    private BigDecimal balance;


}
