package com.glasswallet.company.service.interfaces;

public interface EmailService {
    void sendInvite(String to, String link);
    void sendPasswordReset(String to, String link);
}
