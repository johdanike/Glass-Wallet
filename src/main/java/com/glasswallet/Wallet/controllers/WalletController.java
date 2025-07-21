package com.glasswallet.Wallet.controllers;

import com.glasswallet.Wallet.dtos.requests.CreateWalletRequest;
import com.glasswallet.Wallet.dtos.response.CreateWalletResponse;
import com.glasswallet.Wallet.service.interfaces.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<CreateWalletResponse> createWallet(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateWalletRequest request) {
        String token = authHeader.replace("Bearer ", "");
        CreateWalletResponse response = walletService.createWalletForUser(UUID.fromString(token), request);
        return ResponseEntity.ok(response);
    }
}
