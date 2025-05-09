package org.example.order.core.infra.redis.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.application.security.entity.Oauth2RefreshTokenRedisEntity;
import org.example.order.core.infra.redis.repository.Oauth2TokenRepository;
import org.example.order.core.infra.redis.repository.RedisRepository;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * Redis 기반 RefreshToken 저장소 구현체 + 블랙리스트 기능 포함 (GenericJackson2JsonRedisSerializer 활용).
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class Oauth2TokenRepositoryImpl implements Oauth2TokenRepository {

    private final RedisRepository redisRepository;

    private static final String TOKEN_PREFIX = "oauth2:refresh_token:";
    private static final String BLACKLIST_PREFIX = "oauth2:blacklist:";

    @Override
    public void storeRefreshToken(Oauth2RefreshTokenRedisEntity token) {
        String key = TOKEN_PREFIX + token.getUserId();
        long ttlSeconds = Duration.between(token.getIssuedAt(), token.getExpiresAt()).getSeconds();

        // ✅ JSON 직렬화는 RedisTemplate이 자동 처리
        redisRepository.set(key, token, ttlSeconds);

        log.debug("Stored refresh token for userId={} with TTL={}s", token.getUserId(), ttlSeconds);
    }

    @Override
    public Oauth2RefreshTokenRedisEntity findRefreshToken(String userId) {
        String key = TOKEN_PREFIX + userId;
        Object value = redisRepository.get(key);
        if (value == null) {
            return null;
        }

        // ✅ 이미 객체로 역직렬화되어 반환됨
        return (Oauth2RefreshTokenRedisEntity) value;
    }

    @Override
    public void deleteRefreshToken(String userId) {
        String key = TOKEN_PREFIX + userId;
        redisRepository.delete(key);
        log.debug("Deleted refresh token for userId={}", userId);
    }

    @Override
    public boolean validateRefreshToken(String userId, String tokenValue) {
        Oauth2RefreshTokenRedisEntity entity = findRefreshToken(userId);
        return entity != null && entity.getTokenValue().equals(tokenValue);
    }

    @Override
    public void addToBlacklist(String token, long ttlSeconds) {
        String key = BLACKLIST_PREFIX + token;
        redisRepository.set(key, "blacklisted", ttlSeconds);
        log.debug("Added token to blacklist with TTL={}s", ttlSeconds);
    }

    @Override
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return redisRepository.get(key) != null;
    }
}
