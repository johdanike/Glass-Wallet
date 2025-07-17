package com.glasswallet.Wallet.dtos.requests;

import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.Wallet.enums.WalletType;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class CreateWalletRequest {
    private WalletType type;
    private WalletCurrency currencyType;
}