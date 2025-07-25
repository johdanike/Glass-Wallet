package com.glasswallet.Ledger.service.implementation;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.dtos.responses.SuiResponse;
import com.glasswallet.Ledger.service.interfaces.MoveServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class MoveServiceClientImpl implements MoveServiceClient {

    private final RestTemplate restTemplate;
    private final String moveServiceUrl;

    public MoveServiceClientImpl(RestTemplate restTemplate,
                                 @Value("${glass_wallet_listener}") String moveServiceUrl) {
        this.restTemplate = restTemplate;
        this.moveServiceUrl = moveServiceUrl;
    }

    @Override
    public SuiResponse logOnChain(LedgerEntry entry) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", entry.getSenderId());
        payload.put("receiverId", entry.getReceiverId());
        payload.put("amount", entry.getAmount());
        payload.put("referenceId", entry.getReference());
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
