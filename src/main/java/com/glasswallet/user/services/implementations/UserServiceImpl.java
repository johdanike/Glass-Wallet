package com.glasswallet.user.services.implementations;

import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import com.glasswallet.user.services.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User findOrCreate(String platformId, String platformUserId) {
        return userRepository.findByPlatformIdAndPlatformUserId(platformId, platformUserId)
                .orElseGet(() -> {
                    User user = new User();
                    user.setPlatformId(platformId);
                    user.setPlatformUserId(platformUserId);
                    user.setHasWallet(false);
                    return userRepository.save(user);
                });
    }
}
