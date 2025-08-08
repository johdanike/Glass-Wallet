package com.glasswallet.company.service.implemenation;

import com.glasswallet.company.data.model.ApiKey;
import com.glasswallet.company.data.repo.ApiKeyRepository;
import com.glasswallet.company.dtos.responses.CreateApiKeyResponse;
import com.glasswallet.company.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {
    private final ApiKeyRepository apiKeyRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public CreateApiKeyResponse createApiKey(UUID userId, String name, String ip) {
        String pub = "pk_" + TokenUtils.generateSecureToken(16);
        String secret = "sk_" + TokenUtils.generateSecureToken(40);
        String secretHash = passwordEncoder.encode(secret);

        ApiKey key = ApiKey.builder()
                .id(UUID.randomUUID())
                .publicKey(pub)
                .secretHash(secretHash)
                .userId(userId)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        apiKeyRepo.save(key);

        auditService.log(userId, "API_KEY_CREATED", "Created publicKey=" + pub, ip);
        return new CreateApiKeyResponse(pub, secret);
    }

    public void revokeApiKey(UUID keyId, UUID byUserId, String ip) {
        ApiKey k = apiKeyRepo.findById(keyId).orElseThrow();
        k.setActive(false);
        apiKeyRepo.save(k);
        auditService.log(byUserId, "API_KEY_REVOKED", "Revoked " + k.getPublicKey(), ip);
    }
}
