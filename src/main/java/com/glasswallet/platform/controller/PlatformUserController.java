package com.glasswallet.platform.controller;

import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.platform.dtos.requests.PlatformUserRequest;
import com.glasswallet.platform.dtos.responses.PlatformUserResponse;
import com.glasswallet.platform.service.PlatformUserServiceImpl;
import com.glasswallet.user.data.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform-users")
@RequiredArgsConstructor
public class PlatformUserController {

    private final PlatformUserServiceImpl platformUserServiceImpl;

    @PostMapping("/onboard")
    public ResponseEntity<PlatformUserResponse> onboardPlatformUser(@RequestBody PlatformUserRequest request) {
        PlatformUser platformUser = platformUserServiceImpl.onboardPlatformUser(request);
        User user = platformUser.getUser();

        PlatformUserResponse response = PlatformUserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .companyId(platformUser.getCompanyId())
                .companyUserId(platformUser.getCompanyUserId())
                .build();

        return ResponseEntity.ok(response);
    }

}

