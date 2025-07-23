package com.glasswallet.user.data.repositories;

import com.glasswallet.user.data.models.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPlatformIdAndPlatformUserId(String platformId, String platformUserId);
    User save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID userId);
    Optional<User> findByEmailOrUsernameOrAccountNumber(String email, String username, String accountNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithPessimisticLock(@Param("id") UUID id);
}
