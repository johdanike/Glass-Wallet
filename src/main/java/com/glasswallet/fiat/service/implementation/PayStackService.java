package com.glasswallet.fiat.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.fiat.data.model.PayStackData;
import com.glasswallet.transaction.dtos.request.DepositRequest;
import com.glasswallet.fiat.dtos.request.PayStackRequest;
import com.glasswallet.fiat.dtos.responses.PayStackResponse;
import com.glasswallet.transaction.services.implementations.TransactionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayStackService {
    private final RestTemplate restTemplate;
    private final WebClient.Builder webClientBuilder;
    private final PayStackData payStackData;

    public PayStackResponse initializeTransaction(String email, String amount) {
        String url = payStackData.getBaseUrl() + "/transaction/initialize";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(payStackData.getSecret());

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("amount", amount); // amount in kobo (e.g., 1000 = ₦10)

        // 🔍 Debug logs
        System.out.println("Paystack URL: " + url);
        System.out.println("Authorization: Bearer " + payStackData.getSecret());
        System.out.println("Request Body: " + body);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<PayStackResponse> response = restTemplate.postForEntity(
                url, requestEntity, PayStackResponse.class);

        return response.getBody();
    }


    public PayStackResponse verifyTransaction(String reference) {
        String url = payStackData.getBaseUrl() + "/transaction/verify/" + reference;

        return webClientBuilder.build()
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + payStackData.getSecret())
                .retrieve()
                .bodyToMono(PayStackResponse.class)
                .block();
    }
}
