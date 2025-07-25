package com.glasswallet.security;

import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.user.services.interfaces.CompanyIdentityMapper;
import com.glasswallet.user.dtos.requests.GlassUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class PlatformTokenFilter extends OncePerRequestFilter {

    private final CompanyIdentityMapper identityMapper;
    private final PlatformTokenVerifier tokenVerifier;

    public PlatformTokenFilter(CompanyIdentityMapper identityMapper, PlatformTokenVerifier tokenVerifier) {
        this.identityMapper = identityMapper;
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            PlatformUser platformUser = tokenVerifier.verifyAndExtractUser(token);
            GlassUser glassUser = identityMapper.map(platformUser);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(glassUser, null, List.of());

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            log.info("Token not valid");
        }

        chain.doFilter(request, response);
    }
}