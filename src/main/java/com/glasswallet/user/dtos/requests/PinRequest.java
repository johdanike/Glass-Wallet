package com.glasswallet.user.dtos.requests;

import lombok.Data;

import java.util.UUID;

@Data
public class PinRequest {
    private UUID userId;
    private String pin;
    private String confirmPin;
}