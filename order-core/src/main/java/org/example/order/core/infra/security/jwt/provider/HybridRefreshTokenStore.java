package org.example.order.core.infra.security.jwt.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis + DB 하이브리드 RefreshTokenStore 구현체 (TTL 기반)
 */
@Component
@RequiredArgsConstructor
public class HybridRefreshTokenStore implements RefreshTokenStore {

    private final StringRedisTemplate redisTemplate;

    // 키 접두사 (상수로 통일)
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Override
    public void storeRefreshToken(String userId, String token, long ttlSeconds) {
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + userId, token, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean validateRefreshToken(String userId, String token) {
        String redisToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);

        if (redisToken == null) {
            return false;  // 만료됨 or 존재하지 않음
        }

        return token.equals(redisToken);
    }

    @Override
    public void removeRefreshToken(String userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    @Override
    public boolean isBlacklisted(String token) {
        Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + token);

        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void blacklistToken(String token, long ttlSeconds) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "blacklisted", ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public long getRemainingTtl(String userId) {
        Long ttl = redisTemplate.getExpire(REFRESH_TOKEN_PREFIX + userId, TimeUnit.SECONDS);

        return ttl != null ? ttl : -1;
    }
}
