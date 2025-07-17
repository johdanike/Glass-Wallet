package com.glasswallet.Wallet.exceptions;

public class InvalidCredentialsException extends Throwable {
    public InvalidCredentialsException(String invalidPassword) {
        super("Invalid credentials provided: " + invalidPassword);
    }
}
