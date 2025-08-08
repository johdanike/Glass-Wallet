package com.glasswallet.company.dtos.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String value;
    private String password;
    private String address;
}
