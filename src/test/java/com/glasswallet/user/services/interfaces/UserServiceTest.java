package com.glasswallet.user.services.interfaces;

import static org.junit.jupiter.api.Assertions.*;
import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.platform.service.interfaces.PlatformUserService;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import com.glasswallet.user.dtos.requests.GlassUser;
import com.glasswallet.user.services.implementations.CompanyIdentityMapperImpl;
import com.glasswallet.user.services.implementations.UserLookupService;
import com.glasswallet.user.services.implementations.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class UserServiceTest {
    private UserRepository userRepository;
    private PlatformUserService platformUserService;

    private UserServiceImpl userService;
    private UserLookupService userLookupService;
    private CompanyIdentityMapperImpl identityMapper;

    @BeforeEach
    public void setup() {
        userRepository = mock(UserRepository.class);
        platformUserService = mock(PlatformUserService.class);
//        userService = new UserServiceImpl(userRepository);
        userLookupService = new UserLookupService(platformUserService);
        identityMapper = new CompanyIdentityMapperImpl();
    }

    @Test
    public void findOrCreate_shouldReturnExistingUser() {
        String platformId = "p1";
        String platformUserId = "u1";
        User user = new User();
        user.setPlatformId(platformId);
        user.setPlatformUserId(platformUserId);

        when(userRepository.findByPlatformIdAndPlatformUserId(platformId, platformUserId))
                .thenReturn(Optional.of(user));

        User result = userService.findOrCreate(platformId, platformUserId);

        assertEquals(user, result);
        verify(userRepository, never()).save(any());
    }

    @Test
    public void findOrCreate_shouldCreateNewUserIfNotExists() {
        String platformId = "p2";
        String platformUserId = "u2";

        when(userRepository.findByPlatformIdAndPlatformUserId(platformId, platformUserId))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.findOrCreate(platformId, platformUserId);

        assertEquals(platformId, result.getPlatformId());
        assertEquals(platformUserId, result.getPlatformUserId());
        assertFalse(result.isHasWallet());
    }

    @Test
    public void findOrCreate_shouldSaveOnlyOnce() {
        when(userRepository.findByPlatformIdAndPlatformUserId(any(), any()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(new User());

        userService.findOrCreate("p4", "u4");

        verify(userRepository, times(1)).save(any());
    }

    @Test
    public void findOrCreate_shouldReturnNewUserWithDefaults() {
        when(userRepository.findByPlatformIdAndPlatformUserId(any(), any()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.findOrCreate("p5", "u5");

        assertEquals("p5", user.getPlatformId());
        assertEquals("u5", user.getPlatformUserId());
    }

    @Test
    public void findUser_shouldReturnUser() {
        UUID userId = UUID.randomUUID();
        String companyId = "comp1";
        User user = new User();
        user.setId(userId);

        when(platformUserService.getUserByPlatformUserId(companyId, userId.toString()))
                .thenReturn(user);

        User result = userLookupService.findUser(companyId, userId);
        assertEquals(userId, result.getId());
    }

    @Test
    public void findUser_shouldReturnNullIfNotFound() {
        when(platformUserService.getUserByPlatformUserId(any(), any()))
                .thenReturn(null);

        assertNull(userLookupService.findUser("comp2", UUID.randomUUID()));
    }

    @Test
    public void getInternalWalletId_shouldReturnCorrectId() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(platformUserService.getUserByPlatformUserId(any(), any()))
                .thenReturn(user);

        UUID result = userLookupService.getInternalWalletId("comp3", userId);
        assertEquals(userId, result);
    }

    @Test
    public void getInternalWalletId_shouldReturnNullWhenUserNotFound() {
        when(platformUserService.getUserByPlatformUserId(any(), any()))
                .thenReturn(null);

        UUID result = userLookupService.getInternalWalletId("comp4", UUID.randomUUID());
        assertNull(result);
    }

    @Test
    public void getInternalWalletId_shouldCallGetUserByPlatformUserIdOnce() {
        UUID userId = UUID.randomUUID();
        when(platformUserService.getUserByPlatformUserId(any(), any()))
                .thenReturn(null);

        userLookupService.getInternalWalletId("comp5", userId);

        verify(platformUserService, times(1)).getUserByPlatformUserId("comp5", userId.toString());
    }

    @Test
    public void map_shouldCorrectlyMapPlatformUserToGlassUser() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        PlatformUser platformUser = new PlatformUser();
        platformUser.setUser(user);
        platformUser.setPlatformId("100");
        platformUser.setPlatformUserId("200");

        GlassUser glassUser = identityMapper.map(platformUser);

        assertEquals(userId, glassUser.getId());
        assertEquals(100L, glassUser.getCompanyId());
        assertEquals(200L, glassUser.getCompanyUserId());
    }

    @Test
    public void map_shouldThrowIfPlatformUserUserIsNull() {
        PlatformUser platformUser = new PlatformUser();
        platformUser.setUser(null);
        platformUser.setPlatformId("101");
        platformUser.setPlatformUserId("201");

        assertThrows(NullPointerException.class, () -> identityMapper.map(platformUser));
    }

    @Test
    void getInternalWalletId_shouldThrowUnsupportedOperation() {
        assertThrows(UnsupportedOperationException.class, () ->
                identityMapper.getInternalWalletId("comp6", UUID.randomUUID()));
    }

    @Test
    void map_shouldReturnNonNullGlassUser() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        PlatformUser platformUser = new PlatformUser();
        platformUser.setUser(user);
        platformUser.setPlatformId("300");
        platformUser.setPlatformUserId("400");

        assertNotNull(identityMapper.map(platformUser));
    }

    @Test
    public void map_shouldParseCompanyIdAndUserIdAsLongs() {
        UUID uuid = UUID.randomUUID();
        User user = new User();
        user.setId(uuid);

        PlatformUser platformUser = new PlatformUser();
        platformUser.setUser(user);
        platformUser.setPlatformId("10");
        platformUser.setPlatformUserId("20");

        GlassUser mapped = identityMapper.map(platformUser);

        assertEquals(10L, mapped.getCompanyId());
        assertEquals(20L, mapped.getCompanyUserId());
    }

    @Test
    @DisplayName("toGlassUser is unimplemented and returns null")
    public void toGlassUser_shouldReturnNull() {
        assertNull(identityMapper.toGlassUser("dummyToken"));
    }

    @Test
    void map_shouldFailIfCompanyIdNotNumeric() {
        PlatformUser platformUser = new PlatformUser();
        platformUser.setPlatformId("abc");
        platformUser.setPlatformUserId("123");
        platformUser.setUser(new User());

        assertThrows(NumberFormatException.class, () -> identityMapper.map(platformUser));
    }

    @Test
    public void map_shouldFailIfCompanyUserIdNotNumeric() {
        PlatformUser platformUser = new PlatformUser();
        platformUser.setPlatformId("100");
        platformUser.setPlatformUserId("abc");
        platformUser.setUser(new User());

        assertThrows(NumberFormatException.class, () -> identityMapper.map(platformUser));
    }

    @Test
    public void map_shouldThrowNullPointerIfUserIdIsNull() {
        PlatformUser platformUser = new PlatformUser();
        platformUser.setPlatformId("10");
        platformUser.setPlatformUserId("20");
        User user = new User();
        user.setId(null);
        platformUser.setUser(user);

        GlassUser mapped = identityMapper.map(platformUser);
        assertNull(mapped.getId());
    }

}