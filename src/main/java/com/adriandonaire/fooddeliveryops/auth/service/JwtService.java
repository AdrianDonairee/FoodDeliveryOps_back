package com.adriandonaire.fooddeliveryops.auth.service;

import com.adriandonaire.fooddeliveryops.auth.AppUser;
import com.adriandonaire.fooddeliveryops.auth.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration accessTokenDuration;

    public JwtService(
            @Value("${app.jwt.secret:FoodDeliveryOps_ChangeThisSecret_AtLeast32Chars}") String secret,
            @Value("${app.jwt.access-token-minutes:30}") long accessTokenMinutes) {
        this.signingKey = buildKey(secret);
        this.accessTokenDuration = Duration.ofMinutes(accessTokenMinutes);
    }

    public String generateAccessToken(AppUser user) {
        InstantNow now = InstantNow.now();

        return Jwts.builder()
                .subject(user.getUsername())
                .claims(Map.of("role", user.getRole().toValue()))
                .issuedAt(now.date())
                .expiration(Date.from(now.instant().plus(accessTokenDuration)))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UserRole extractRole(String token) {
        String role = parseClaims(token).get("role", String.class);
        return UserRole.fromValue(role);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (Exception exception) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey buildKey(String secret) {
        byte[] keyBytes = secret.length() >= 32
                ? secret.getBytes(StandardCharsets.UTF_8)
                : Decoders.BASE64.decode(secret);

        return Keys.hmacShaKeyFor(keyBytes);
    }

    private record InstantNow(Date date, java.time.Instant instant) {
        static InstantNow now() {
            java.time.Instant instant = java.time.Instant.now();
            return new InstantNow(Date.from(instant), instant);
        }
    }
}