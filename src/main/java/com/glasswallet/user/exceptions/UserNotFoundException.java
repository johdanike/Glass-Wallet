package com.glasswallet.user.exceptions;


import com.glasswallet.exceptions.GlassWalletException;

public class UserNotFoundException extends GlassWalletException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
