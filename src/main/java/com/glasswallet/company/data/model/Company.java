package com.glasswallet.company.data.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "companies")
@Data
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Changed to UUID
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, name = "company_name")
    private String name;

    @Column(nullable = false, unique = true, name = "platform_id")
    private String platformId;

    @Column(nullable = false, name = "password") // Removed unique for flexibility
    private String password;

    private String industry;

    @Column(nullable = false, unique = true, name = "email")
    private String email;

    private String website;
    private String phoneNumber;
    private boolean receiveUpdate;
}