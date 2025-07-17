package com.glasswallet.user.exceptions;


import com.glasswallet.exceptions.GlassWalletException;

public class PasswordLenghtMismatchException extends GlassWalletException {
    public PasswordLenghtMismatchException(String message) {
        super(message);
    }
}
