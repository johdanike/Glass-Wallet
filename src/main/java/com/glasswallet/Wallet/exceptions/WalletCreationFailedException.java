package com.glasswallet.Wallet.exceptions;

import com.glasswallet.exceptions.GlassWalletException;

public class WalletCreationFailedException extends GlassWalletException {
    public WalletCreationFailedException(String message) {
        super(message);
    }
}
