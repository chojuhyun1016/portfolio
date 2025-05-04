package org.example.order.core.infra.redis.repository;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

/**
 * RefreshToken Redis Repository 인터페이스
 * - RefreshToken 관련 Redis 조작 기능 정의
 */
public interface RefreshTokenRedisRepository {

    void save(String key, String token, long ttl, TimeUnit timeUnit);

    Optional<String> findByKey(String key);

    void delete(String key);

    boolean isBlacklisted(String token);

    void blacklist(String token, long ttl, TimeUnit timeUnit);

    OptionalLong getRemainingTtl(String key, TimeUnit timeUnit);
}