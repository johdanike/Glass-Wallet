package com.glasswallet.company.service.interfaces;

import java.util.UUID;

public interface ApiService {
    String generateApiKey(String companyName, String platformId);
    String generateSecretKey();
    UUID validateApiKey(String apiKey, String secretKey);
}
