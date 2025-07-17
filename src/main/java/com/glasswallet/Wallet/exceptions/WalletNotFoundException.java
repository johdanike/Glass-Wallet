package com.glasswallet.Wallet.exceptions;


import com.glasswallet.exceptions.GlassWalletException;

public class WalletNotFoundException extends GlassWalletException {
    public WalletNotFoundException(String message) {
        super(message);
    }
}
