package com.glasswallet.platform.service;

import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.platform.data.repositories.PlatformUserRepository;
import com.glasswallet.platform.dtos.requests.PlatformUserRequest;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformUserServiceImpl implements PlatformUserService {

    private final PlatformUserRepository platformUserRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PlatformUser onboardPlatformUser(PlatformUserRequest request) {
        // Implementation
        return null;
    }

    @Override
    public User getUserByPlatformUserId(String companyId, String companyUserId) {
        // Implementation
        return null;
    }
}