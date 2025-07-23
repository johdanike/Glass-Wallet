// src/main/java/com/glasswallet/Ledger/service/implementation/MoveServiceClientImpl.java
package com.glasswallet.Ledger.service.implementation;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.dtos.responses.SuiResponse;
import com.glasswallet.Ledger.service.interfaces.MoveServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MoveServiceClientImpl implements MoveServiceClient {
    private final RestTemplate restTemplate;
    @Value("${glass_wallet_listener}")
    private final String moveServiceUrl = "http://localhost:3000/api/transactions"; // or env/config

    @Override
    public SuiResponse logOnChain(LedgerEntry entry) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", entry.getSenderId());
        payload.put("receiverId", entry.getReceiverId());
        payload.put("amount", entry.getAmount());
        payload.put("reference", entry.getReference());
        payload.put("companyId", entry.getCompanyId());
        payload.put("currency", entry.getCurrency());
        payload.put("type", entry.getType().toString());

        ResponseEntity<SuiResponse> response = restTemplate.postForEntity(
                moveServiceUrl,
                payload,
                SuiResponse.class
        );

        return response.getBody();
    }
}