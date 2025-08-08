package com.glasswallet.company.service.interfaces;

import com.glasswallet.company.dtos.request.LoginRequest;
import com.glasswallet.company.dtos.responses.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);
//    AuthResponse login(String value, String password, String remoteAddr);
}
