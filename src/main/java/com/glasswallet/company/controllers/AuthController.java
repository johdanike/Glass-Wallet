package com.glasswallet.company.controllers;

import com.glasswallet.company.dtos.request.LoginRequest;
import com.glasswallet.company.dtos.responses.AuthResponse;
import com.glasswallet.company.service.implemenation.InviteService;
import com.glasswallet.company.service.interfaces.AuthService;
import com.glasswallet.company.service.interfaces.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final InviteService inviteService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        AuthResponse res = authService.login(req);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgot(@RequestBody Map<String,String> body, HttpServletRequest req) {
        passwordResetService.requestReset(body.get("email"), req.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> reset(@RequestBody Map<String,String> body, HttpServletRequest req) {
        passwordResetService.resetPassword(body.get("token"), body.get("password"), req.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invite/accept")
    public ResponseEntity<Void> acceptInvite(@RequestBody Map<String,String> body, HttpServletRequest req) {
        inviteService.acceptInvite(body.get("token"), body.get("password"), body.get("fullName"), req.getRemoteAddr());
        return ResponseEntity.ok().build();
    }
}
