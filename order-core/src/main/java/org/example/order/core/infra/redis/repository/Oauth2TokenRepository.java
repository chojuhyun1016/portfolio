package org.example.order.core.infra.redis.repository;

import org.example.order.core.application.security.entity.Oauth2RefreshTokenRedisEntity;

/**
 * RefreshToken 저장소 인터페이스 + 블랙리스트 기능.
 */
public interface Oauth2TokenRepository {
    void storeRefreshToken(Oauth2RefreshTokenRedisEntity token);
    Oauth2RefreshTokenRedisEntity findRefreshToken(String userId);
    void deleteRefreshToken(String userId);

    boolean validateRefreshToken(String userId, String tokenValue);

    // === AccessToken 블랙리스트 기능 ===
    void addToBlacklist(String token, long ttlSeconds);
    boolean isBlacklisted(String token);
}
