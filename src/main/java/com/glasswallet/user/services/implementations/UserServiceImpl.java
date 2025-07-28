package com.glasswallet.user.services.implementations;

import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.data.repositories.WalletRepository;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.company.data.model.Company;
import com.glasswallet.company.data.repo.CompanyRepo;
import com.glasswallet.company.service.interfaces.ApiService;
import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.platform.data.repositories.PlatformUserRepository;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import com.glasswallet.user.dtos.requests.WalletActivationRequest;
import com.glasswallet.user.dtos.responses.WalletActivationResponse;
import com.glasswallet.user.services.interfaces.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PlatformUserRepository platformUserRepository;
    private final WalletRepository walletRepository;
    private final CompanyRepo companyRepository;
    private final ApiService apiKeyService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findOrCreate(String platformId, String platformUserId) {
        return userRepository.findByPlatformIdAndPlatformUserId(platformId, platformUserId)
                .orElseGet(() -> {
                    User user = new User();
                    user.setPlatformId(platformId);
                    user.setPlatformUserId(platformUserId);
                    user.setHasWallet(false);
                    return userRepository.save(user);
                });
    }

    @Transactional
    public WalletActivationResponse activateWallet(String apiKey, String apiSecret, WalletActivationRequest request) {
        Company company = companyRepository.findById(apiKeyService.validateApiKey(apiKey, apiSecret))
                .orElseThrow(() -> new SecurityException("Invalid API credentials"));
        UUID companyId = company.getId();

        if (userRepository.findByEmail(request.getEmail()).isPresent())
            throw new IllegalArgumentException("Email already registered");
        User user = getUser(request, companyId);
        user = userRepository.save(user);

        PlatformUser platformUser = getPlatformUserAndSave(user, companyId);

        return getWalletActivationResponse(user, platformUser);
    }

    private PlatformUser getPlatformUserAndSave(User user, UUID companyId) {
        PlatformUser platformUser = new PlatformUser();
        platformUser.setId(UUID.randomUUID());
        platformUser.setPlatformUserId(user.getId().toString()); // Unique user ID
        platformUser.setPlatformId(companyId.toString()); // Link to company
        platformUser.setUser(user);
        platformUser.setBalanceFiat(BigDecimal.ZERO); // Default fiat balance
        platformUser.setBalanceSui(BigDecimal.ZERO);  // Default crypto balance
        platformUser = platformUserRepository.save(platformUser);

        createAndSaveNewWalletResponse(user);
        return platformUser;
    }

    private void createAndSaveNewWalletResponse(User user) {
        Wallet wallet = new Wallet();
        wallet.setWalletAddress(generateUniqueWalletAddress());
        wallet.setBalance(BigDecimal.ZERO);
//        wallet.setAccountNumber(Wallet.generateAccountNumber());
        wallet.setCurrencyType(WalletCurrency.SUI);
        wallet.setUser(user);
        walletRepository.save(wallet);
    }

    private static WalletActivationResponse getWalletActivationResponse(User user, PlatformUser platformUser) {
        WalletActivationResponse response = new WalletActivationResponse();
        response.setUserId(String.valueOf(user.getId()));
        response.setPlatformUserId(String.valueOf(platformUser.getId()));
        response.setMessage("Wallet activated successfully");
        return response;
    }

    private User getUser(WalletActivationRequest request, UUID companyId) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setHasWallet(true);
        user.setPlatformId(companyId.toString());
        user.setOnboarded(true);
        user.setCreatedAt(Instant.now());
        user.setPlatformUserId(request.getPlatformUserId());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setLastName(request.getLastName());
        return user;
    }

    private String generateUniqueWalletAddress() {
        return "0x" + UUID.randomUUID().toString().replace("-", "");
    }

}
