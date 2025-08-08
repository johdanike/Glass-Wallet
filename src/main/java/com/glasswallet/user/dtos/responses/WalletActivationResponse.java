package com.glasswallet.user.dtos.responses;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WalletActivationResponse {
    private String userId;
    private String platformUserId;
    private String message;
}
