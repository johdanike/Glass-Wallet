package com.glasswallet.company.controllers;

import com.glasswallet.company.dtos.request.CompanySignupRequest;
import com.glasswallet.company.dtos.responses.CompanySignupResponse;
import com.glasswallet.company.service.interfaces.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class CompanyControllers {
    private final CompanyService companyService;

    @PostMapping("/signup")
    public ResponseEntity<CompanySignupResponse> signup(@RequestBody CompanySignupRequest request) {
        CompanySignupResponse response = companyService.signup(request);
        return ResponseEntity.ok(response);
    }

}
