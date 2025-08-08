package com.glasswallet.company.data.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OnboardedUser {
//    @Id
//    @Column(columnDefinition = "BINARY(16)")
////    @Type(type = "org.hibernate.type.UUIDBinaryType")
//    private UUID id;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable=false, unique=true)
    private String email;

    @Column(nullable=false)
    private String password;

    @Column(name="full_name")
    private String fullName;

    @Column(name="is_active")
    private boolean active = true;

    @Column(name="is_email_verified")
    private boolean emailVerified = false;

    @Column(name="last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name="user_id"),
            inverseJoinColumns = @JoinColumn(name="role_id"))
    private Set<Role> roles;
}
