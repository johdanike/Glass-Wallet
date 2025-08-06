package com.glasswallet.transaction.controller;

import com.glasswallet.transaction.dtos.request.PayStackRequest;
import com.glasswallet.transaction.dtos.response.DepositResponse;
import com.glasswallet.transaction.dtos.response.PayStackResponse;
import com.glasswallet.transaction.services.implementations.PayStackService;
import com.glasswallet.transaction.services.implementations.TransactionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/paystack")
@RequiredArgsConstructor
public class PayStackController {

    private final PayStackService paystackService;
    private final TransactionServiceImpl transactionServiceImpl;

    @PostMapping("/paystack/initiate-deposit")
    public ResponseEntity<?> initiateFiatDeposit(@RequestBody PayStackRequest request) {
        return ResponseEntity.ok(paystackService.initializeTransaction(request));
    }
    @GetMapping("/paystack/verify")
    public ResponseEntity<?> verifyPaystackDeposit(@RequestParam("reference") String reference, @RequestParam("receiverId") UUID receiverId) {
        boolean success = paystackService.verifyTransactionAndCreditUser(reference, receiverId);
        return ResponseEntity.ok( Map.of("success", success));
    }
    @PostMapping("/verify-payment")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body) {
        String reference = body.get("reference");
        PayStackResponse response = paystackService.verifyTransaction(reference);

        if (response.isStatus() && "success".equalsIgnoreCase(response.getData().getStatus())) {
            // credit user's wallet
            return ResponseEntity.ok("Payment verified and wallet credited");
        }
        return ResponseEntity.badRequest().body("Payment verification failed");
    }






}
