package com.glasswallet.user.controllers;

import com.glasswallet.Wallet.service.interfaces.WalletService;
import com.glasswallet.user.dtos.requests.GlassUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallet")
@Slf4j
public class UserController {

    private final WalletService glassWalletService;

    public UserController(WalletService glassWalletService) {
        this.glassWalletService = glassWalletService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserWalletProfile(Authentication auth) {
        GlassUser user = (GlassUser) auth.getPrincipal(); // Set by PlatformTokenFilter
        com.glasswallet.user.dtos.responses.WalletProfileDto profile = glassWalletService.getProfile(user.getId());
        return ResponseEntity.ok(profile);
    }
}
