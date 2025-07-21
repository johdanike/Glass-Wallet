package com.glasswallet.user.services.implementations;

import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.dtos.requests.GlassUser;
import com.glasswallet.user.services.interfaces.CompanyIdentityMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CompanyIdentityMapperImpl implements CompanyIdentityMapper {

    @Override
    public GlassUser toGlassUser(String token) {
        // Implement if needed
        return null;
    }

    @Override
    public UUID getInternalWalletId(String companyId, UUID userId) {
        // This method should be moved to a different service
        throw new UnsupportedOperationException("Method moved to UserLookupService");
    }

    @Override
    public GlassUser map(PlatformUser platformUser) {
        User user = platformUser.getUser();
        return GlassUser.builder()
                .id(user.getId())
                .companyId(Long.parseLong(platformUser.getCompanyId()))
                .companyUserId(Long.parseLong(platformUser.getCompanyUserId()))
                .build();
    }
}