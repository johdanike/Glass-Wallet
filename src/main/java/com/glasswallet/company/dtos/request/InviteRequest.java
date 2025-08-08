package com.glasswallet.company.dtos.request;

import lombok.Data;

@Data
public class InviteRequest {
    private String email;
    private String role;
}