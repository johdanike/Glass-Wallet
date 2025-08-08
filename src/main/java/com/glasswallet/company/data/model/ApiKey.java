package com.glasswallet.company.data.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name="public_key", unique=true)
    private String publicKey;

    @Column(name="secret_hash")
    private String secretHash;

    @Column(columnDefinition = "BINARY(16)")
    private UUID userId;

    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
