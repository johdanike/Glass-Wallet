package com.glasswallet.company.data.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="password_reset_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(unique=true)
    private String token;

    private LocalDateTime expiresAt;
    private boolean used;
    private LocalDateTime createdAt;
}
