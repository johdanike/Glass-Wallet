package com.glasswallet.company.data.repo;

import com.glasswallet.company.data.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByPublicKey(String publicKey);
    List<ApiKey> findAllByUserId(UUID userId);
}