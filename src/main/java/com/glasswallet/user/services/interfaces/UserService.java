package com.glasswallet.user.services.interfaces;

import com.glasswallet.user.data.models.User;
import com.glasswallet.user.dtos.responses.LoginResponse;
import org.springframework.stereotype.Component;

@Component
public interface UserService {

    LoginResponse generateLoginResponse(User user, String message);
}
