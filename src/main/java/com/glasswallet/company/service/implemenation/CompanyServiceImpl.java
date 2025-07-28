package com.glasswallet.company.service.implemenation;

import com.glasswallet.company.data.model.Company;
import com.glasswallet.company.data.repo.CompanyRepo;
import com.glasswallet.company.dtos.request.CompanySignupRequest;
import com.glasswallet.company.dtos.responses.CompanySignupResponse;
import com.glasswallet.company.service.interfaces.CompanyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepo companyRepo;


    @Override
    @Transactional
    public CompanySignupResponse signup(CompanySignupRequest request) {
        if (companyRepo.findByName(request.getCompanyName()) != null){
            throw new RuntimeException("Company already exists");
        }
        Company  company = new Company();
        company.setId( UUID.randomUUID() );
        company.setName(request.getCompanyName());
        company.setPlatformId( company.getId().toString() );
         company = companyRepo.save(company);

//         String apiKey = generateApiKey(company.getId());
//        String secretKey = generateSecretKey();



         CompanySignupResponse response = new CompanySignupResponse();
        response.setCompanyId(company.getId());
        response.setApiKey(response.getApiKey());
        response.setSecretKey(response.getSecretKey());
        return response;



    }
}
