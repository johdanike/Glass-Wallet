package com.glasswallet.Ledger.exceptions;

import com.glasswallet.exceptions.GlassWalletException;

public class TypeNotFoundException extends GlassWalletException {
    public TypeNotFoundException(String typeNotFound) {
        super(typeNotFound);
    }
}
