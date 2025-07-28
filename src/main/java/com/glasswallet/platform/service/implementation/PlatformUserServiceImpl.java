package com.glasswallet.platform.service.implementation;

import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.platform.data.repositories.PlatformUserRepository;
import com.glasswallet.platform.dtos.requests.PlatformUserRequest;
import com.glasswallet.platform.exceptions.NotFoundException;
import com.glasswallet.platform.service.interfaces.PlatformUserService;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformUserServiceImpl implements PlatformUserService {

    private final PlatformUserRepository platformUserRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PlatformUser onboardPlatformUser(PlatformUserRequest request) {
        User user = findOrCreateUser(request);

        return platformUserRepository.findByPlatformIdAndPlatformUserId(
                        request.getCompanyId(), request.getCompanyUserId())
                .orElseGet(() -> createPlatformUser(request, user));
    }

    // Helper Method 1
    private User findOrCreateUser(PlatformUserRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(request.getEmail());
                    newUser.setFirstName(request.getFirstName());
                    newUser.setLastName(request.getLastName());
                    newUser.setPhoneNumber(request.getPhoneNumber());
                    newUser.setUsername(request.getUserName());
                    newUser.setPlatformUserId(request.getCompanyUserId());
                    newUser.setPlatformId(request.getCompanyId());
                    newUser.setPreferredCurrency(request.getCurrency());
                    newUser.setOnboarded(true);
                    newUser.setCreatedAt(Instant.now());
                    return userRepository.save(newUser);
                });
    }

    // Helper Method 2
    private PlatformUser createPlatformUser(PlatformUserRequest request, User user) {
        PlatformUser platformUser = new PlatformUser();
        platformUser.setId(UUID.randomUUID());
        platformUser.setPlatformId(request.getCompanyId());
        platformUser.setPlatformUserId(request.getCompanyUserId());
        platformUser.setUser(user);
        platformUser.setToken(request.getToken());
        return platformUserRepository.save(platformUser);
    }


    @Override
    public User getUserByPlatformUserId(String companyId, String companyUserId) {
        return platformUserRepository.findByPlatformIdAndPlatformUserId(companyId, companyUserId)
                .map(PlatformUser::getUser)
                .orElseThrow(() -> new NotFoundException("Platform user not found"));
    }
}