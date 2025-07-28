package com.glasswallet.company.service.implemenation;

import com.glasswallet.company.data.model.Company;
import com.glasswallet.company.data.repo.CompanyRepo;
import com.glasswallet.company.dtos.request.CompanySignupRequest;
import com.glasswallet.company.dtos.responses.CompanySignupResponse;
import com.glasswallet.company.service.interfaces.ApiService;
import com.glasswallet.company.service.interfaces.CompanyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepo companyRepo;
    private final ApiService apiService;
    private final PasswordEncoder passwordEncoder; // Inject PasswordEncoder

    @Override
    @Transactional
    public CompanySignupResponse signup(CompanySignupRequest request) {
        log.info("Attempting to onboard company: {}", request.getCompanyName());
        try {
            if (companyRepo.findByName(request.getCompanyName()).isPresent()) {
                log.warn("Company already exists: {}", request.getCompanyName());
                throw new RuntimeException("Company already exists");
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                log.warn("Password is required for company: {}", request.getCompanyName());
                throw new RuntimeException("Password is required");
            }

            Company company = new Company();
            company.setId(UUID.randomUUID());
            company.setName(request.getCompanyName());
            company.setPlatformId(company.getId().toString());
            company.setIndustry(request.getIndustry());
            company.setReceiveUpdate(false);
            company.setPhoneNumber(request.getPhoneNumber());
            company.setEmail(request.getEmail());
            company.setPassword(passwordEncoder.encode(request.getPassword())); // Hash password
            company = companyRepo.save(company);

            String apiKey = apiService.generateApiKey(company.getName(), company.getPlatformId());
            String secretKey = apiService.generateSecretKey();

            CompanySignupResponse response = new CompanySignupResponse();
            response.setCompanyId(company.getId());
            response.setApiKey(apiKey);
            response.setSecretKey(secretKey);
            log.info("Company onboarded successfully: {} (ID: {})", company.getName(), company.getId());
            return response;
        } catch (Exception e) {
            log.error("Error during company onboarding for {}: {}", request.getCompanyName(), e.getMessage());
            throw e;
        }
    }
}