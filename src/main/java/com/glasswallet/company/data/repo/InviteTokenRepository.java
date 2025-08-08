package com.glasswallet.company.data.repo;

import com.glasswallet.company.data.model.InviteToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface InviteTokenRepository extends JpaRepository<InviteToken, UUID> {
    Optional<InviteToken> findByToken(String token);
    void deleteByUsedTrueAndExpiresAtBefore(LocalDateTime date);
}