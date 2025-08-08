package com.glasswallet.company.service.implemenation;

import com.glasswallet.company.data.model.Company;
import com.glasswallet.company.data.repo.CompanyRepo;
import com.glasswallet.company.dtos.request.CompanySignupRequest;
import com.glasswallet.company.dtos.responses.CompanySignupResponse;
import com.glasswallet.company.service.interfaces.ApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CompanyServiceImplTest {
    @Mock
    private CompanyRepo companyRepo;
    @Mock
    private ApiService apiService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private CompanyServiceImpl companyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void signup_success() {
        CompanySignupRequest request = new CompanySignupRequest();
        request.setCompanyName("TestCo");
        request.setEmail("test@example.com");
        request.setPassword("password");
        when(companyRepo.findByName(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(companyRepo.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(apiService.generateApiKey(anyString(), anyString())).thenReturn("api-key");
        when(apiService.generateSecretKey()).thenReturn("secret-key");

        CompanySignupResponse response = companyService.signup(request);
        assertNotNull(response.getCompanyId());
        assertEquals("api-key", response.getApiKey());
        assertEquals("secret-key", response.getSecretKey());
        verify(companyRepo).save(any(Company.class));
    }

    @Test
    void signup_existingCompany_throwsException() {
        CompanySignupRequest request = new CompanySignupRequest();
        request.setCompanyName("TestCo");
        request.setEmail("test@example.com");
        request.setPassword("password");
        when(companyRepo.findByName(anyString())).thenReturn(Optional.of(new Company()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> companyService.signup(request));
        assertEquals("Company already exists", ex.getMessage());
    }

    @Test
    void signup_missingPassword_throwsException() {
        CompanySignupRequest request = new CompanySignupRequest();
        request.setCompanyName("TestCo");
        request.setEmail("test@example.com");
        request.setPassword("");
        when(companyRepo.findByName(anyString())).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> companyService.signup(request));
        assertEquals("Password is required", ex.getMessage());
    }
}

