package org.example.order.core.infra.security.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final StringRedisTemplate redisTemplate;

    private String getKey(String userId) {
        return "refresh_token:" + userId;
    }

    public void save(String userId, String token, long expireSeconds) {
        redisTemplate.opsForValue().set(getKey(userId), token, expireSeconds, TimeUnit.SECONDS);
    }

    public String get(String userId) {
        return redisTemplate.opsForValue().get(getKey(userId));
    }

    public void delete(String userId) {
        redisTemplate.delete(getKey(userId));
    }
}
