package com.digitallibrary.service.impl;

import com.digitallibrary.entity.AppUser;
import com.digitallibrary.entity.RefreshToken;
import com.digitallibrary.exception.AuthenticationException;
import com.digitallibrary.repository.RefreshTokenRepository;
import com.digitallibrary.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(AppUser user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshExpirationDays));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            throw new AuthenticationException("Refresh token was revoked.");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new AuthenticationException("Refresh token has expired. Please log in again.");
        }
        return token;
    }

    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(String refreshTokenStr) {
        RefreshToken existingToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token."));

        if (existingToken.isRevoked()) {
            log.warn("SECURITY ALERT: Attempted reuse of revoked refresh token for user {}. Revoking all tokens!", existingToken.getUser().getEmail());
            refreshTokenRepository.revokeAllByUserId(existingToken.getUser().getId());
            throw new AuthenticationException("Security violation detected: token reuse. Account tokens revoked.");
        }

        verifyExpiration(existingToken);

        // Mark existing token revoked and record replacement
        String newTokenStr = UUID.randomUUID().toString();
        existingToken.setRevoked(true);
        existingToken.setReplacedByToken(newTokenStr);
        refreshTokenRepository.save(existingToken);

        // Issue new refresh token
        RefreshToken newToken = new RefreshToken();
        newToken.setUser(existingToken.getUser());
        newToken.setToken(newTokenStr);
        newToken.setExpiresAt(LocalDateTime.now().plusDays(refreshExpirationDays));
        newToken.setRevoked(false);

        return refreshTokenRepository.save(newToken);
    }

    @Override
    @Transactional
    public void revokeUserTokens(Long userId) {
        if (userId != null) {
            refreshTokenRepository.revokeAllByUserId(userId);
        }
    }

    @Override
    @Transactional
    public void revokeToken(String tokenStr) {
        if (tokenStr != null && !tokenStr.isBlank()) {
            refreshTokenRepository.findByToken(tokenStr).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }
}
