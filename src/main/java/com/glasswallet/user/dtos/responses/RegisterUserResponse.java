package com.glasswallet.user.dtos.responses;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
//@RequiredArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterUserResponse {
    private boolean success;
    private String message;
    private String suiAddress;
    private String username;
    private int statusCode;
    private Map<String, Object> responseBody;


    public RegisterUserResponse(boolean b, String loginSuccessful, String suiAddress, String userName) {
        this.success = b;
        this.message = loginSuccessful;
        this.suiAddress = suiAddress;
        this.username = userName;
    }

    public RegisterUserResponse(boolean isSuccess, String registrationSuccessful, String suiAddress, String userName, int value, Map<String, Object> walletDetails) {
        this.success = isSuccess;
        this.message = registrationSuccessful;
        this.suiAddress = suiAddress;
        this.username = userName;
        this.statusCode = value;
        this.responseBody = walletDetails;
    }
}
