package com.glasswallet.user.controllers;

import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.platform.data.repositories.PlatformUserRepository;
import com.glasswallet.transaction.services.implementations.TransactionServiceImpl;
import com.glasswallet.user.dtos.requests.LoginRequest;
import com.glasswallet.user.dtos.requests.PinRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PlatformUserRepository platformUserRepository;
    private final TransactionServiceImpl transactionService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for userId: {}", request.getUserId());
        try {
            PlatformUser user = platformUserRepository.findByPlatformUserId(request.getUserId().toString())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (!user.isActivated()) {
                log.warn("Login failed for userId {}: Wallet not activated", request.getUserId());
                return ResponseEntity.status(403).body("Please activate your wallet by setting a PIN.");
            }
            log.info("Login successful for userId: {}", request.getUserId());
            return ResponseEntity.ok("Login successful");
        } catch (Exception e) {
            log.error("Login error for userId {}: {}", request.getUserId(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/set-pin")
    public ResponseEntity<?> setPin(@RequestBody PinRequest request) {
        log.info("PIN set attempt for userId: {}", request.getUserId());
        try {
            PlatformUser user = platformUserRepository.findByPlatformUserId(request.getUserId().toString())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (user.isActivated()) {
                log.warn("PIN set failed for userId {}: Wallet already activated", request.getUserId());
                return ResponseEntity.badRequest().body("Wallet already activated");
            }

            if (request.getPin().length() != 4 || !request.getPin().matches("\\d{4}")) {
                log.warn("PIN set failed for userId {}: Invalid PIN format", request.getUserId());
                return ResponseEntity.badRequest().body("PIN must be a 4-digit number");
            }

            if (!request.getPin().equals(request.getConfirmPin())) {
                log.warn("PIN set failed for userId {}: PINs do not match", request.getUserId());
                return ResponseEntity.badRequest().body("PINs do not match");
            }

            user.setPin(request.getPin());
            user.setActivated(true);
            platformUserRepository.save(user);

            log.info("PIN set and wallet activated for userId: {}", request.getUserId());
            return ResponseEntity.ok("Wallet activated successfully");
        } catch (Exception e) {
            log.error("PIN set error for userId {}: {}", request.getUserId(), e.getMessage());
            throw e;
        }
    }
}
