package com.glasswallet.company.data.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invite_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InviteToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    private String email;
    @Column(unique=true)
    private String token;
    private String role;
    private LocalDateTime expiresAt;
    private boolean used;
    private LocalDateTime createdAt;
}
