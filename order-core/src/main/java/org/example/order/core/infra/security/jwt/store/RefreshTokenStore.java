package org.example.order.core.infra.security.jwt.store;

/**
 * Refresh Token 스토어 인터페이스 (외부 인프라 계층에서 구현)
 */
public interface RefreshTokenStore {

    void storeRefreshToken(String userId, String token, long ttlSeconds);

    boolean validateRefreshToken(String userId, String token);

    void removeRefreshToken(String userId);

    boolean isBlacklisted(String token);

    void blacklistToken(String token, long ttlSeconds);

    long getRemainingTtl(String userId);
}
