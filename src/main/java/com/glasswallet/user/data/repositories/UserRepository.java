package com.glasswallet.user.data.repositories;

import com.glasswallet.user.data.models.User;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(@NotEmpty(message = "phone number cannot be empty") @Pattern(regexp = "^\\+?[0-9\\s]{7,14}$",
            message = "Invalid phone number format") String phoneNumber);
    
    @Query("SELECT w.user FROM Wallet w WHERE w.accountNumber = :accountNumber")
    Optional<User> findUserByAccountNumber(@Param("accountNumber") String accountNumber);
    
    @Query("SELECT w.user FROM Wallet w WHERE w.walletAddress = :walletAddress")
    Optional<User> findUserByWalletAddress(@Param("walletAddress") String walletAddress);

    Optional<User> findByEmailOrUsername(String identifier, String identifier1);
}