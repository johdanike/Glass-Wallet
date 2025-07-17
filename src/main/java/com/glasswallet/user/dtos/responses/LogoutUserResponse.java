package com.glasswallet.user.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LogoutUserResponse {
    private String message;
    private boolean isLoggedIn;
}
