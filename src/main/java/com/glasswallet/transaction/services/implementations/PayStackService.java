package com.glasswallet.transaction.services.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.transaction.data.models.PayStackData;
import com.glasswallet.transaction.dtos.request.DepositRequest;
import com.glasswallet.transaction.dtos.request.PayStackRequest;
import com.glasswallet.transaction.dtos.response.PayStackResponse;
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
    @Value("${PAY_STACK_SECRET_KEY}")
    private String secretKey;

    private final RestTemplate restTemplate;
    private final TransactionServiceImpl transactionService;
    private final WalletCurrency currency = WalletCurrency.NGN;
    private final PayStackData payStackData;



    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.paystack.co")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer" + secretKey )
            .build();

    public PayStackResponse verifyTransaction(String reference) {
        return webClient.get()
                .uri("/transaction/verify/" + reference)
                .retrieve()
                .bodyToMono(PayStackResponse.class)
                .block();
    }


    public PayStackResponse initializeTransaction(PayStackRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set( "Authorization", "Bearer " + secretKey ); // Inject using @Value
        headers.setContentType( MediaType.APPLICATION_JSON );

        Map<String, Object> body = new HashMap<>();
        body.put( "email", request.getEmail() );
        body.put( "amount", request.getAmount().multiply( BigDecimal.valueOf( 100 ) ).intValue() ); // Paystack uses kobo
        body.put( "callback_url", "https://your-app.com/api/v1/transactions/paystack/verify" );

        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>( body, headers );
        ResponseEntity<PayStackResponse> response = restTemplate.postForEntity(
                "https://api.paystack.co/transaction/initialize",
                httpRequest,
                PayStackResponse.class
        );

        return response.getBody();
    }
    public boolean verifyTransactionAndCreditUser(String reference, UUID receiverId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + secretKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "https://api.paystack.co/transaction/verify/" + reference,
                HttpMethod.GET,
                request,
                JsonNode.class
        );

        JsonNode body = response.getBody();
        if (body != null && body.get("data").get("status").asText().equals("success")) {
            BigDecimal amount = new BigDecimal(body.get("data").get("amount").asText()).divide(BigDecimal.valueOf(100));
            DepositRequest deposit = new DepositRequest();
            deposit.setAmount(amount);
            deposit.setCurrency(WalletCurrency.NGN);
            deposit.setReceiverId(receiverId);
            transactionService.processDeposit(deposit);
            return true;
        }
        return false;
    }
    public String createPayStackRecipient(String name, String accountNumber, String bankCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "type", "nuban",
                "name", name,
                "account_number", accountNumber,
                "bank_code", bankCode,
                "currency", "NGN"
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "https://api.paystack.co/transferrecipient",
                request,
                JsonNode.class
        );

        return response.getBody().get("data").get("recipient_code").asText();
    }
    public void initiateTransfer(String recipientCode, BigDecimal amount, String reason) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("source", "balance");
        body.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
        body.put("recipient", recipientCode);
        body.put("reason", reason);

        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(body, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "https://api.paystack.co/transfer",
                httpRequest,
                JsonNode.class
        );

        if (!"success".equals(response.getBody().get("status").asText())) {
            throw new RuntimeException("Failed to initiate Paystack transfer");
        }
    }


}
