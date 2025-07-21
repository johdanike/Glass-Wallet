package com.glasswallet.user.services.interfaces;

import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.user.dtos.requests.GlassUser;

import java.util.UUID;

public interface CompanyIdentityMapper {
    GlassUser toGlassUser(String token);
    UUID getInternalWalletId(String companyId, UUID userId);
    GlassUser map(PlatformUser platformUser);
}