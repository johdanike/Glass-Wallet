package com.glasswallet.user.dtos.requests;

import lombok.Data;

import java.util.UUID;

@Data
public class LoginRequest {
    private UUID userId;
    private String password;
}