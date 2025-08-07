package com.glasswallet.fiat.controllers;

import com.glasswallet.fiat.data.model.PayStackData;
import com.glasswallet.fiat.dtos.request.PayStackRequest;
import com.glasswallet.fiat.dtos.responses.PayStackResponse;
import com.glasswallet.fiat.service.implementation.PayStackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/payments")

public class PaymentController {
    private final PayStackService payStackService;

    @PostMapping("/initialize")
    public ResponseEntity<PayStackResponse> initTransaction(@RequestBody PayStackRequest request) {
        PayStackResponse response = payStackService.initializeTransaction(
                request.getEmail(),
                request.getAmount().toString()
        );

        return ResponseEntity.ok(response);
    }
    @PostMapping("/verify")
    public ResponseEntity<?> verifyTransaction(@RequestBody Map<String, String> request) {
        String reference = request.get("reference");
        PayStackResponse response = payStackService.verifyTransaction(reference);
        return ResponseEntity.ok(response);
    }
}