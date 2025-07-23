package com.glasswallet.platform.controller;

import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.platform.dtos.requests.PlatformUserRequest;
import com.glasswallet.platform.dtos.responses.PlatformUserResponse;
import com.glasswallet.platform.service.PlatformUserService;
import com.glasswallet.platform.service.PlatformUserServiceImpl;
import com.glasswallet.user.data.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform-users")
@RequiredArgsConstructor
public class PlatformUserController {

    private final PlatformUserService platformUserService;

    @PostMapping("/onboard")
    public ResponseEntity<PlatformUserResponse> onboardPlatformUser(@RequestBody PlatformUserRequest request) {
        PlatformUser platformUser = platformUserService.onboardPlatformUser(request);
        User user = platformUser.getUser();

        PlatformUserResponse response = PlatformUserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .companyId(platformUser.getPlatformId())
                .companyUserId(platformUser.getPlatformUserId())
                .build();

        return ResponseEntity.ok(response);
    }

//    @PostMapping("/onboard")
//    public ResponseEntity<PlatformUser> onboardPlatformUser(@RequestBody PlatformUserRequest request) {
//        PlatformUser user = platformUserService.onboardPlatformUser(request);
//        return ResponseEntity.ok(user);
//    }

    @GetMapping("/{companyId}/{companyUserId}")
    public ResponseEntity<User> getUserByPlatformUserId(
            @PathVariable String companyId,
            @PathVariable String companyUserId
    ) {
        User user = platformUserService.getUserByPlatformUserId(companyId, companyUserId);
        return ResponseEntity.ok(user);
    }

}

