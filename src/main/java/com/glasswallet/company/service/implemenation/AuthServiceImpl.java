package com.glasswallet.company.service.implemenation;

import com.glasswallet.company.data.model.OnboardedUser;
import com.glasswallet.company.data.model.Role;
import com.glasswallet.company.data.repo.OnboardedUserRepository;
import com.glasswallet.company.dtos.request.LoginRequest;
import com.glasswallet.company.dtos.responses.AuthResponse;
import com.glasswallet.company.service.interfaces.AuthService;
import com.glasswallet.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authManager;
    private final OnboardedUserRepository onboardedUserRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AuthServiceImpl(AuthenticationManager authManager, OnboardedUserRepository onboardedUserRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.authManager = authManager;
        this.onboardedUserRepository = onboardedUserRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(loginRequest.getValue(), loginRequest.getPassword());
        authManager.authenticate(authToken);

        OnboardedUser user = onboardedUserRepository.findByEmail(loginRequest.getValue()).orElseThrow();
        user.setLastLoginAt(LocalDateTime.now());
        onboardedUserRepository.save(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles().stream().map(Role::getName).toList());
        String token = jwtUtil.generateToken(user.getEmail(), claims);
        String ip =

        auditService.log(user.getId(), "LOGIN_SUCCESS", "Login success", loginRequest.getAddress());
        return new AuthResponse(token, Long.parseLong(String.valueOf(jwtUtil.getExpirationMs())));

    }



}
