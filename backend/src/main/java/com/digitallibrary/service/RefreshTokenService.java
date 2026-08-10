package com.digitallibrary.service;

import com.digitallibrary.entity.AppUser;
import com.digitallibrary.entity.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(AppUser user);
    RefreshToken verifyExpiration(RefreshToken token);
    RefreshToken rotateRefreshToken(String refreshTokenStr);
    void revokeUserTokens(Long userId);
    void revokeToken(String tokenStr);
}
