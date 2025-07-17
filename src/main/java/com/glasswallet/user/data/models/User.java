package com.glasswallet.user.data.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.glasswallet.user.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "users")
@JsonIgnoreProperties(ignoreUnknown = true)
@RequiredArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", columnDefinition = "VARCHAR(36)", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.REGULAR;

    private boolean isLoggedIn = false;
    private boolean hasWallet = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;


    public String getFullName() {
        return firstName + " " + lastName;
    }
    public String getDisplayName() {
        return username != null && !username.isEmpty() ? username : getFullName();
    }
    public String setUsername(String firstName, String lastName) {
        if (firstName == null || lastName == null) {
            return null;
        }
        String username = firstName.toLowerCase() + "." + lastName.toLowerCase();
        this.username = username;
        return username;
    }

}