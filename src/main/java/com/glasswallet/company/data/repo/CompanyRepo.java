package com.glasswallet.company.data.repo;

import com.glasswallet.company.data.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRepo extends JpaRepository<Company, UUID> {
    Company findByName(String name);
    Company findByPlatformUserId(UUID platformUserId);
}
