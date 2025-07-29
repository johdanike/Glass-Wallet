package com.glasswallet.user.dtos.requests;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class WalletActivationRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String userName;
    private Map<String, String> kycDetails;
    private String platformUserId;
}
