package org.example.order.core.infra.security.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.security.auth.dto.AuthTokenResponse;
import org.example.order.core.infra.security.auth.store.AuthTokenStoreService;
import org.example.order.core.infra.security.auth.store.RefreshTokenStoreService;
import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 인증 토큰 발급, 재발급, 블랙리스트 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private static final String ROLE_USER = "ROLE_USER";

    private final JwtTokenManager jwtTokenManager;
    private final RefreshTokenStoreService refreshTokenStoreService;
    private final AuthTokenStoreService authTokenStoreService;

    /**
     * 액세스/리프레시 토큰 발급
     */
    public AuthTokenResponse issueTokens(String userId, String device, String aud, List<String> scopes, String ip) {
        String jti = UUID.randomUUID().toString();

        String accessToken = jwtTokenManager.createAccessToken(userId, List.of(ROLE_USER), jti, device, aud, scopes, ip);
        String refreshToken = jwtTokenManager.createRefreshToken(userId, jti);

        // 저장
        refreshTokenStoreService.saveRefreshToken(userId, refreshToken, jwtTokenManager.getRemainingSeconds(refreshToken));
        authTokenStoreService.storeJti(jti, jwtTokenManager.getRemainingSeconds(accessToken));

        return new AuthTokenResponse(accessToken, refreshToken);
    }

    /**
     * RefreshToken을 통한 재발급
     */
    public AuthTokenResponse reissueTokens(String userId, String oldRefreshToken, String device, String aud, List<String> scopes, String ip) {
        String storedToken = refreshTokenStoreService.getRefreshToken(userId);
        if (storedToken == null || !storedToken.equals(oldRefreshToken)) {
            throw new IllegalStateException("Refresh token mismatch");
        }

        return issueTokens(userId, device, aud, scopes, ip);
    }

    /**
     * 로그아웃 처리
     */
    public void logout(String userId, String accessToken) {
        refreshTokenStoreService.deleteRefreshToken(userId);

        long ttl = jwtTokenManager.getRemainingSeconds(accessToken);
        if (ttl > 0) {
            authTokenStoreService.blacklistAccessToken(accessToken, ttl);
        }
    }

    /**
     * 액세스 토큰 블랙리스트 여부 조회
     */
    public boolean isAccessTokenBlacklisted(String accessToken) {
        return authTokenStoreService.isBlacklisted(accessToken);
    }
}
