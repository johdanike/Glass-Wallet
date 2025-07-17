package com.glasswallet.Wallet.exceptions;


import com.glasswallet.exceptions.GlassWalletException;

public class WalletAlreadyExistsException extends GlassWalletException {
    public WalletAlreadyExistsException(String message) {
        super(message);
    }
}
