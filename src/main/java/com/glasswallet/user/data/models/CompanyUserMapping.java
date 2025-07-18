package com.glasswallet.user.data.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
public class CompanyUserMapping {
    @Id
    private UUID id;

    private String companyId;
    private UUID platformUserId;
    private String email;

    private UUID internalWalletId;
}
