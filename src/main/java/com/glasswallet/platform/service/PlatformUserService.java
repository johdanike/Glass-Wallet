package com.glasswallet.platform.service;

import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.platform.dtos.requests.PlatformUserRequest;
import com.glasswallet.user.data.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public interface PlatformUserService {
    @Transactional
    PlatformUser onboardPlatformUser(PlatformUserRequest request);

    User getUserByPlatformUserId(String companyId, String companyUserId);
}
