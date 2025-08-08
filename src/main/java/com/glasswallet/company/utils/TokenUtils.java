package com.glasswallet.company.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenUtils {
    private static final SecureRandom random = new SecureRandom();

    public static String generateSecureToken(int length) {
        byte[] b = new byte[length];
        random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
