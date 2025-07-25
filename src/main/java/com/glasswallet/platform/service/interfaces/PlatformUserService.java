package com.glasswallet.platform.service.interfaces;

import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.platform.dtos.requests.PlatformUserRequest;
import com.glasswallet.user.data.models.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public interface PlatformUserService {
    @Transactional
    PlatformUser onboardPlatformUser(PlatformUserRequest request);

    User getUserByPlatformUserId(String companyId, String companyUserId);
}
