package com.glasswallet.company.data.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(columnDefinition = "BINARY(16)")
    private UUID userId;

    private String action;
    @Column(columnDefinition = "TEXT")
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
