package com.glasswallet.user.services.interfaces;


import com.glasswallet.user.dtos.requests.CreateNewUserRequest;
import com.glasswallet.user.dtos.requests.LoginRequest;
import com.glasswallet.user.dtos.requests.LogoutRequest;
import com.glasswallet.user.dtos.responses.CreateNewUserResponse;
import com.glasswallet.user.dtos.responses.LoginResponse;
import com.glasswallet.user.dtos.responses.LogoutUserResponse;

public interface AuthService {
    CreateNewUserResponse createNewUser(CreateNewUserRequest newUserRequest);
    LoginResponse login(LoginRequest loginRequest);

    LoginResponse refreshAccessToken(String refreshTokenStr);

    LogoutUserResponse logOut(LogoutRequest logOutRequest);
}
