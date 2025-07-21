package com.glasswallet.platform.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data

public class PlatformUserResponse {
    private UUID userId;
    private String email;
    private String phoneNumber;
    private String fullName;
    private String companyId;
    private String companyUserId;
}


