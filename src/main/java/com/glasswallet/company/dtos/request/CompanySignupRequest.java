package com.glasswallet.company.dtos.request;

import lombok.Data;

@Data
public class CompanySignupRequest {
    private String companyName;
    private String password;
    private String industry;
    private String phoneNumber;
    private String email;
}