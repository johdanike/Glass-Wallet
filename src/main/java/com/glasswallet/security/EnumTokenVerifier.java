package com.glasswallet.security;

import com.glasswallet.platform.data.models.PlatformUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class EnumTokenVerifier {

    @Value("${jwt_secrets.enum_token_secret}")
    private String secret;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public PlatformUser verifyAndExtractUser(String token) {
        Jws<Claims> claimsJws = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);

        Claims claims = claimsJws.getBody();

        PlatformUser platformUser = new PlatformUser();
        platformUser.setPlatformId(claims.get("companyId", String.class));
        platformUser.setPlatformUserId(claims.get("companyUserId", String.class));

        return platformUser;
    }

    public PlatformUser extractUser(String token) {
        return verifyAndExtractUser(token);
    }
}
