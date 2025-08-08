package com.glasswallet.company.service.interfaces;


public interface PasswordResetService {
    void requestReset(String email, String remoteAddr);
    void resetPassword(String token, String password, String remoteAddr);
}
