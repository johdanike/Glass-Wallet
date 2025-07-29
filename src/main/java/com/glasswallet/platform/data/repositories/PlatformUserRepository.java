package com.glasswallet.platform.data.repositories;

import com.glasswallet.platform.data.models.PlatformUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PlatformUserRepository extends JpaRepository<PlatformUser, UUID> {
    Optional<PlatformUser> findByPlatformIdAndPlatformUserId(String platformId, String platformUserId);

    boolean existsByPlatformIdAndPlatformUserId(String platformId, String platformUserId);
    @Query("SELECT p FROM PlatformUser p WHERE p.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PlatformUser> findByIdWithPessimisticLock(UUID id);

    @Query("SELECT COALESCE(p.balanceSui, 0) + COALESCE(p.balanceFiat, 0) FROM PlatformUser p WHERE p.id = :id")
    BigDecimal getBalance(UUID id);

    boolean existsByGeneratedPlatformUserId(String candidateId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PlatformUser> findByPlatformUserId(String platformUserId);
}
