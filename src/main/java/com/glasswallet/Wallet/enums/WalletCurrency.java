package com.glasswallet.Wallet.enums;

import lombok.Getter;

@Getter
public enum WalletCurrency {
    NGN(WalletType.FIAT),
    SUI(WalletType.CRYPTO);

    private final WalletType walletType;

    WalletCurrency(WalletType walletType) {
        this.walletType = walletType;
    }
}
