package com.glasswallet.user.data.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"platformId", "platformUserId"})
})
@Data
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "platform_id", nullable = false)
    private String platformId;

    @Column(name = "platform_user_id", nullable = false, unique = true)
    private String platformUserId;

    private boolean hasWallet;

    private Instant createdAt;
    private Instant lastSeenAt;

    private String preferredCurrency;
    private boolean onboarded;
    @PreUpdate
    public void preUpdate() {
        lastSeenAt = Instant.now();
    }

}
