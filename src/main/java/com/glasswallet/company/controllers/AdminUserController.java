package com.glasswallet.company.controllers;

import com.glasswallet.company.data.model.OnboardedUser;
import com.glasswallet.company.data.model.Role;
import com.glasswallet.company.data.repo.OnboardedUserRepository;
import com.glasswallet.company.data.repo.RoleRepository;
import com.glasswallet.company.dtos.request.InviteRequest;
import com.glasswallet.company.service.implemenation.AuditService;
import com.glasswallet.company.service.implemenation.InviteService;
import com.glasswallet.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final InviteService inviteService;
    private final OnboardedUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final AuditService auditService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> invite(@RequestBody InviteRequest req, Authentication auth, HttpServletRequest httpReq) {
        UUID byUserId = ((UserPrincipal) auth.getPrincipal()).getId();
        inviteService.invite(req.getEmail(), req.getRole(), byUserId, httpReq.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<OnboardedUser> listUsers() {
        return userRepo.findAll();
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignRole(@PathVariable UUID id, @RequestBody Map<String,String> body, Authentication auth, HttpServletRequest httpReq) {
        OnboardedUser user = userRepo.findById(id).orElseThrow();
        Role role = roleRepo.findByName(body.get("role"));
        user.setRoles(Set.of(role));
        userRepo.save(user);
        auditService.log(((UserPrincipal)auth.getPrincipal()).getId(), "ROLE_CHANGED", "Changed role for " + id + " to " + role.getName(), httpReq.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id, Authentication auth, HttpServletRequest httpReq) {
        OnboardedUser onboardedUser = userRepo.findById(id).orElseThrow();
        onboardedUser.setActive(false);
        userRepo.save(onboardedUser);
        auditService.log(((UserPrincipal)auth.getPrincipal()).getId(), "USER_DEACTIVATED", "Deactivated " + id, httpReq.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
