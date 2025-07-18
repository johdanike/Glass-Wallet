package com.glasswallet.user.data.repositories;

import com.glasswallet.user.data.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPlatformIdAndPlatformUserId(String platformId, String platformUserId);

    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID userId);

    Optional<User> findByEmailOrUsernameOrAccountNumber(String email, String username, String accountNumber);
}
