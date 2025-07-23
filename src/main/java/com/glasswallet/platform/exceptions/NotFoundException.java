package com.glasswallet.platform.exceptions;

import com.glasswallet.exceptions.GlassWalletException;


public class NotFoundException extends GlassWalletException {
    public NotFoundException(String message) {
        super(message);
    }
}
