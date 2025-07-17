package com.glasswallet.user.controllers;

import com.glasswallet.exceptions.GlassWalletException;
import com.glasswallet.security.JwtUtil;
import com.glasswallet.user.data.models.RefreshToken;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.dtos.requests.CreateNewUserRequest;
import com.glasswallet.user.dtos.requests.LoginRequest;
import com.glasswallet.user.dtos.requests.LogoutRequest;
import com.glasswallet.user.dtos.responses.CreateNewUserResponse;
import com.glasswallet.user.dtos.responses.LoginResponse;
import com.glasswallet.user.dtos.responses.LogoutUserResponse;
import com.glasswallet.user.services.implementations.RefreshTokenService;
import com.glasswallet.user.services.interfaces.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<CreateNewUserResponse> register(
            @Valid @RequestBody CreateNewUserRequest request
    ) {
        try {
            log.info("Registration attempt for email: {}", request.getEmail());
            request.setEmail(request.getEmail().trim());
            request.setFirstName(request.getFirstName().trim());
            request.setLastName(request.getLastName().trim());
            request.setEmail(request.getEmail().trim());
            request.setPhoneNumber(request.getPhoneNumber().trim());
            
            CreateNewUserResponse response = authService.createNewUser(request);
            log.info("User registered successfully: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (GlassWalletException e) {
            log.error("Registration failed for email {}: {}", request.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during registration for email {}: {}", request.getEmail(), e.getMessage(), e);
            throw new GlassWalletException("Registration failed due to an unexpected error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Login attempt for email: {}", request.getEmail());
            LoginResponse response = authService.login(request);
            log.info("User logged in successfully: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (GlassWalletException e) {
            log.error("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login for email {}: {}", request.getEmail(), e.getMessage());
            throw new GlassWalletException("Login failed due to an unexpected error");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutUserResponse> logout(@Valid @RequestBody LogoutRequest request) {
        try {
            log.info("Logout attempt for email: {}", request.getEmail());
            LogoutUserResponse response = authService.logOut(request);
            log.info("User logged out successfully: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (GlassWalletException e) {
            log.error("Logout failed for email {}: {}", request.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during logout for email {}: {}", request.getEmail(), e.getMessage());
            throw new GlassWalletException("Logout failed due to an unexpected error");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String token = request.get("refreshToken");

        RefreshToken refreshToken = refreshTokenService
                .findByToken(token)
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        User user = refreshToken.getUser();

        refreshTokenService.revokeAllUserTokens(user);
        RefreshToken newToken = refreshTokenService.createRefreshToken(user);

        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole());

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newToken.getToken()
        ));
    }
}
