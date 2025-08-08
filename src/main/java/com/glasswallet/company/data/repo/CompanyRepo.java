package com.glasswallet.company.data.repo;

import com.glasswallet.company.data.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepo extends JpaRepository<Company, UUID> {
    Optional<Company> findByName(String name); // Updated to Optional
    Optional<Company> findByPlatformId(String platformId); // Updated to String and Optional
}