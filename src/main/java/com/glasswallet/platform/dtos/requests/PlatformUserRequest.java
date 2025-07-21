package com.glasswallet.platform.dtos.requests;

import lombok.Data;

@Data
public class PlatformUserRequest {
    private String companyId;        // e.g. "enum"
    private String companyUserId;    // e.g. "enum-abc-123"
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String userName;
}
