package org.example.order.core.infra.redis.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.redis.repository.RefreshTokenRedisRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

/**
 * Redis를 통한 Refresh Token 관리 리포지토리 구현체.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepositoryImpl implements RefreshTokenRedisRepository {

    private final StringRedisTemplate redisTemplate;

    // Redis 키 접두사
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Refresh Token 저장 (TTL 포함)
     */
    @Override
    public void save(String key, String token, long ttl, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + key, token, ttl, timeUnit);
    }

    /**
     * Refresh Token 조회
     */
    @Override
    public Optional<String> findByKey(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + key));
    }

    /**
     * Refresh Token 삭제
     */
    @Override
    public void delete(String key) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + key);
    }

    /**
     * 토큰이 블랙리스트에 있는지 여부 확인
     */
    @Override
    public boolean isBlacklisted(String token) {
        Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + token);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 토큰을 블랙리스트에 등록 (TTL 포함)
     */
    @Override
    public void blacklist(String token, long ttl, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "blacklisted", ttl, timeUnit);
    }

    /**
     * Refresh Token의 남은 TTL 조회
     */
    @Override
    public OptionalLong getRemainingTtl(String key, TimeUnit timeUnit) {
        long ttl = redisTemplate.getExpire(REFRESH_TOKEN_PREFIX + key, timeUnit);

        // Spring Data Redis:
        // - ttl >= 0: 정상 TTL
        // - ttl == -1: 만료 없음
        // - ttl == -2: 키 없음
        if (ttl >= 0) {
            return OptionalLong.of(ttl);
        } else {
            return OptionalLong.empty();
        }
    }
}
