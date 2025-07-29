package com.glasswallet.company.service.implemenation;

import com.glasswallet.company.data.repo.CompanyRepo;
import com.glasswallet.company.service.interfaces.ApiService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ApiServiceImpl implements ApiService {
    private final CompanyRepo companyRepo;

    public ApiServiceImpl(CompanyRepo companyRepo) {
        this.companyRepo = companyRepo;
    }

    @Override
    public String generateApiKey(String companyName, String platformId) {
        return companyName + "-" + platformId + "-" + UUID.randomUUID();
    }

    @Override
    public String generateSecretKey() {
        return UUID.randomUUID().toString();
    }

    @Override
    public UUID validateApiKey(String apiKey, String secretKey) {
        String [] parts = apiKey.split("-");
        if (parts.length != 2) throw new IllegalArgumentException("Invalid API key format");
        return UUID.fromString(parts[0].replace("company-", ""));
    }
}
