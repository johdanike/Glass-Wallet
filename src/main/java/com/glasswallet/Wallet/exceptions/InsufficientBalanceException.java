package com.glasswallet.Wallet.exceptions;


import com.glasswallet.exceptions.GlassWalletException;

public class InsufficientBalanceException extends GlassWalletException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
