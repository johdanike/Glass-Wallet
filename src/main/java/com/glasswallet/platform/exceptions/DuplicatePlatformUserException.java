package com.glasswallet.platform.exceptions;

public class DuplicatePlatformUserException extends RuntimeException {
    
    public DuplicatePlatformUserException(String message) {
        super(message);
    }
    
    public DuplicatePlatformUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
