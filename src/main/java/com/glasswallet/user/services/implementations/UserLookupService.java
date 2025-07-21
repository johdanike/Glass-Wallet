package com.glasswallet.user.services.implementations;

import com.glasswallet.platform.service.PlatformUserService;
import com.glasswallet.user.data.models.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserLookupService {

    private final PlatformUserService platformUserService;

    public UserLookupService(PlatformUserService platformUserService) {
        this.platformUserService = platformUserService;
    }

    public User findUser(String companyId, UUID userId) {
        return platformUserService.getUserByPlatformUserId(companyId, userId.toString());
    }

    public UUID getInternalWalletId(String companyId, UUID userId) {
        User user = findUser(companyId, userId);
        return user != null ? user.getId() : null;
    }
}
