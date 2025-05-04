package org.example.order.core.infra.security.jwt.store;

/**
 * Refresh Token 스토어 인터페이스 (Redis 또는 DB 기반 구현체 가능)
 */
public interface RefreshTokenStore {

    void storeRefreshToken(String userId, String token, long ttlSeconds);

    boolean validateRefreshToken(String userId, String token);

    void removeRefreshToken(String userId);

    boolean isBlacklisted(String token);

    void blacklistToken(String token, long ttlSeconds);

    long getRemainingTtl(String userId);
}
