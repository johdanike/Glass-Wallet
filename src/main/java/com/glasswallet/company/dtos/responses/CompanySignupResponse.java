package com.glasswallet.company.dtos.responses;

import lombok.Data;

import java.util.UUID;

@Data
public class CompanySignupResponse {
    private UUID companyId;
    private String apiKey;
    private String secretKey;
}
