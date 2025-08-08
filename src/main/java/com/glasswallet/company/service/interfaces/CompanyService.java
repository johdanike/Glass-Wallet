package com.glasswallet.company.service.interfaces;

import com.glasswallet.company.dtos.request.CompanySignupRequest;
import com.glasswallet.company.dtos.responses.CompanySignupResponse;

public interface CompanyService {
    CompanySignupResponse signup(CompanySignupRequest request);

}
