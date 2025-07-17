package com.glasswallet.user.services.implementations;

import com.glasswallet.exceptions.GlassWalletException;
import com.glasswallet.security.JwtUtil;
import com.glasswallet.user.data.models.RefreshToken;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import com.glasswallet.user.dtos.requests.CreateNewUserRequest;
import com.glasswallet.user.dtos.requests.LoginRequest;
import com.glasswallet.user.dtos.requests.LogoutRequest;
import com.glasswallet.user.dtos.responses.CreateNewUserResponse;
import com.glasswallet.user.dtos.responses.LoginResponse;
import com.glasswallet.user.dtos.responses.LogoutUserResponse;
import com.glasswallet.user.enums.Role;
import com.glasswallet.user.exceptions.PasswordLenghtMismatchException;
import com.glasswallet.user.exceptions.UserNotFoundException;
import com.glasswallet.user.services.interfaces.AuthService;
import com.glasswallet.user.services.interfaces.UserService;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder, UserService userService,
                           JwtUtil jwtUtil,
                           RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    @Override
    public CreateNewUserResponse createNewUser(CreateNewUserRequest request) {
        validateSignUpRequest(request);
        String email = request.getEmail().toLowerCase().trim();
        String phone = normalizePhoneNumber(request.getPhoneNumber());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        if (userRepository.existsByPhoneNumber(phone)) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(phone);
        user.setRole(Role.REGULAR);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
        return new CreateNewUserResponse("User created successfully", user.getId().toString(), email, phone);
    }

    @Transactional
    @Override
    public LoginResponse login(LoginRequest request) {
        // Validate email first
        if (isNullOrEmpty(request.getEmail())) {
            throw new UserNotFoundException("Email is required");
        }
        
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with that email doesn't exist"));

        if (request.getPassword() == null) {
            throw new IllegalArgumentException("Password is required");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect credentials");
        }

        user.setLoggedIn(true);
        user.setLastLoginAt(LocalDateTime.now());
        user.setHasWallet(true);
        userRepository.save(user);

        refreshTokenService.createRefreshToken(user);
        
        return userService.generateLoginResponse(user, "Logged in successfully");
    }

    @Override
    public LoginResponse refreshAccessToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new GlassWalletException("Invalid or expired refresh token"));

        User user = refreshToken.getUser();
        refreshTokenService.revokeAllUserTokens(user);
        refreshTokenService.createRefreshToken(user);

        return userService.generateLoginResponse(user, "Token refreshed successfully");
    }

    @Override
    public LogoutUserResponse logOut(LogoutRequest request) {
        // Validate email first
        if (isNullOrEmpty(request.getEmail())) {
            throw new UserNotFoundException("Email is required");
        }
        
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with that email doesn't exist"));

        if (!user.isLoggedIn()) {
            throw new IllegalArgumentException("User is already logged out");
        }

        user.setLoggedIn(false);
        userRepository.save(user);
        return new LogoutUserResponse("Logged Out Successfully", false);
    }


    private void validateSignUpRequest(CreateNewUserRequest request) {
        if (isNullOrEmpty(request.getFirstName())) throw new IllegalArgumentException("First name is required");
        if (isNullOrEmpty(request.getLastName())) throw new IllegalArgumentException("Last name is required");
        if (isNullOrEmpty(request.getEmail())) throw new IllegalArgumentException("Email is required");
        if (isNullOrEmpty(request.getPhoneNumber())) throw new IllegalArgumentException("Phone number is required");
        if (isNullOrEmpty(request.getPassword())) throw new IllegalArgumentException("Password is required");

        if (request.getPassword().length() < 8)
            throw new PasswordLenghtMismatchException("Password must be between 8 and 20 characters");
        // Trim email before validation
        String trimmedEmail = request.getEmail() != null ? request.getEmail().trim() : "";
        if (!trimmedEmail.matches("^[\\w+.'\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$"))
            throw new IllegalArgumentException("Invalid email format");
        if (request.getPassword().contains(" "))
            throw new IllegalArgumentException("Password must not contain spaces");
    }

    private boolean isNullOrEmpty(String input) {
        return input == null || input.trim().isEmpty();
    }

    private String normalizePhoneNumber(String phone) {
        try {
            return phoneUtil.format(phoneUtil.parse(phone, "NG"), PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }

}






