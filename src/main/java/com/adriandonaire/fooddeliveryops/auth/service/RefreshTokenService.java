package com.adriandonaire.fooddeliveryops.auth.service;

import com.adriandonaire.fooddeliveryops.auth.AppUser;
import com.adriandonaire.fooddeliveryops.auth.RefreshToken;
import com.adriandonaire.fooddeliveryops.auth.repository.RefreshTokenRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshTokenDuration;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-token-days:7}") long refreshTokenDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenDuration = Duration.ofDays(refreshTokenDays);
    }

    @Transactional
    public RefreshToken issueToken(AppUser user) {
        String token = generateTokenValue();
        RefreshToken refreshToken = new RefreshToken(token, user, Instant.now().plus(refreshTokenDuration));
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken rotateToken(RefreshToken token) {
        token.revoke();
        refreshTokenRepository.save(token);
        return issueToken(token.getUser());
    }

    @Transactional
    public void revokeToken(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue).ifPresent(refreshToken -> {
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);
        });
    }

    @Transactional(readOnly = true)
    public RefreshToken requireValidToken(String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido."));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new IllegalArgumentException("Refresh token vencido o revocado.");
        }

        return refreshToken;
    }

    private String generateTokenValue() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}