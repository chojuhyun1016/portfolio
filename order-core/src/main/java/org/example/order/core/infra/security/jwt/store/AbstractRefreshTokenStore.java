package org.example.order.core.infra.security.jwt.store;

/**
 * RefreshTokenStore 기본 스켈레톤
 * - CRUD(저장, 삭제 등)는 외부에서 구현하도록 위임.
 * - 핵심 로직(메서드 시그니처 등)만 제공.
 */
public abstract class AbstractRefreshTokenStore implements RefreshTokenStore {

    @Override
    public void storeRefreshToken(String userId, String token, long ttlSeconds) {
        throw new UnsupportedOperationException("storeRefreshToken must be implemented by subclass");
    }

    @Override
    public boolean validateRefreshToken(String userId, String token) {
        throw new UnsupportedOperationException("validateRefreshToken must be implemented by subclass");
    }

    @Override
    public void removeRefreshToken(String userId) {
        throw new UnsupportedOperationException("removeRefreshToken must be implemented by subclass");
    }

    @Override
    public boolean isBlacklisted(String token) {
        throw new UnsupportedOperationException("isBlacklisted must be implemented by subclass");
    }

    @Override
    public void blacklistToken(String token, long ttlSeconds) {
        throw new UnsupportedOperationException("blacklistToken must be implemented by subclass");
    }

    @Override
    public long getRemainingTtl(String userId) {
        throw new UnsupportedOperationException("getRemainingTtl must be implemented by subclass");
    }
}
