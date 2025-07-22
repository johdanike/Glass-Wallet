package com.glasswallet.platform.data.repositories;

import com.glasswallet.platform.data.models.PlatformUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlatformUserRepository extends JpaRepository<PlatformUser, UUID> {
    Optional<PlatformUser> findByPlatformIdAndPlatformUserId(String platformId, String platformUserId);

    boolean existsPlatformIdAndPlatformUserId(String platformId, String platformUserId);
}
