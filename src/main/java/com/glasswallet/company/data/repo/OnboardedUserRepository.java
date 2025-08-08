package com.glasswallet.company.data.repo;

import com.glasswallet.company.data.model.OnboardedUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OnboardedUserRepository extends JpaRepository<OnboardedUser, UUID> {
    Optional<OnboardedUser> findByEmail(String email);

}
