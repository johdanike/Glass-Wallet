package com.glasswallet.user.dtos.responses;

import com.glasswallet.user.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.UUID;

@Data
@Builder
@ToString
@AllArgsConstructor
public class LoginResponse {
    private String message;
    private String accessToken;
    private String refreshToken;
    private Role role;
    private boolean loggedIn;
    private String userId;
    private boolean hasSuiWallet;
    private boolean hasFiatWallet;
    private String fiatWalletAccountNumber;
    private String suiWalletAddress;

}


