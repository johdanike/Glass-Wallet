package com.glasswallet.platform.data.repositories;

import com.glasswallet.platform.data.models.PlatformUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlatformUserRepository extends JpaRepository<PlatformUser, UUID> {
    Optional<PlatformUser> findByCompanyIdAndCompanyUserId(String companyId, String companyUserId);

    boolean existsByCompanyIdAndCompanyUserId(String companyId, String companyUserId);
}
