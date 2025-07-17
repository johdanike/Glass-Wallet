package com.glasswallet.Wallet.service.interfaces;

import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.enums.WalletCurrency;

import java.util.Optional;

public interface WalletResolver {
    Optional<Wallet> resolveWallet(String identifier, WalletCurrency currencyType);
}
