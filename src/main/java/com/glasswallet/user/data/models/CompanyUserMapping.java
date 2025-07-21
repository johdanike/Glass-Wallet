package com.glasswallet.user.data.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
public class CompanyUserMapping {
    @Id
    private UUID id;
    @Column(name = "company_id",  nullable = false)
    private String companyId;

    @Column(name = "platform_user_id",   nullable = false)
    private UUID platformUserId;
    private String email;
    private UUID internalWalletId;
}
