package com.glasswallet.company.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateApiKeyResponse {
    private String publicKey;
    private String secret; // show only once
}