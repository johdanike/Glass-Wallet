package com.glasswallet.company.service.implemenation;

import com.glasswallet.company.data.model.InviteToken;
import com.glasswallet.company.data.model.OnboardedUser;
import com.glasswallet.company.data.repo.InviteTokenRepository;
import com.glasswallet.company.data.repo.OnboardedUserRepository;
import com.glasswallet.company.data.repo.RoleRepository;
import com.glasswallet.company.service.interfaces.EmailService;
import com.glasswallet.company.utils.TokenUtils;
import com.glasswallet.company.data.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteService {
    private final InviteTokenRepository inviteRepo;
    private final OnboardedUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${invite.token-expiry-minutes}")
    private long inviteExpiryMinutes;

    @Value("${invite.accept.base-url}")
    private String inviteAcceptBaseUrl;

    public void invite(String email, String roleName, UUID byUserId, String ip) {
        Optional<OnboardedUser> existing = userRepo.findByEmail(email);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }

        String token = TokenUtils.generateSecureToken(48);
        InviteToken it = InviteToken.builder()
                .id(UUID.randomUUID())
                .email(email)
                .token(token)
                .role(roleName)
                .expiresAt(LocalDateTime.now().plusMinutes(inviteExpiryMinutes))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        inviteRepo.save(it);

        String link = inviteAcceptBaseUrl + token;
        emailService.sendInvite(email, link);

        auditService.log(byUserId, "INVITE_CREATED", "Invited " + email + " role=" + roleName, ip);
    }

    public OnboardedUser acceptInvite(String token, String password, String fullName, String ip) {
        InviteToken invite = inviteRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite token"));

        if (invite.isUsed() || invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invite token expired or already used");
        }

        Role role = roleRepo.findByName(invite.getRole()) ;

        OnboardedUser user = OnboardedUser.builder()
                .id(UUID.randomUUID())
                .email(invite.getEmail())
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .active(true)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .roles(Set.of(role))
                .build();

        userRepo.save(user);

        invite.setUsed(true);
        inviteRepo.save(invite);

        auditService.log(user.getId(), "INVITE_ACCEPTED", "Accepted invite from token", ip);

        return user;
    }

}
