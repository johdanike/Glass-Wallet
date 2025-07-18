package com.glasswallet.user.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class GlassUser {
    private UUID id;
    private Long companyId;
    private Long companyUserId;

}