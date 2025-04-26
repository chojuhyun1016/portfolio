package org.example.order.core.infra.security.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthTokenStoreService {

    private final StringRedisTemplate redisTemplate;

    public void storeJti(String jti, long ttlSeconds) {
        redisTemplate.opsForValue().set("jti:" + jti, "valid", ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean isJtiValid(String jti) {
        return redisTemplate.hasKey("jti:" + jti);
    }

    public void storeRefreshToken(String userId, String refreshToken, long ttlSeconds) {
        redisTemplate.opsForValue().set("refresh:" + userId, refreshToken, ttlSeconds, TimeUnit.SECONDS);
    }

    public String getRefreshToken(String userId) {
        return redisTemplate.opsForValue().get("refresh:" + userId);
    }

    public void deleteRefreshToken(String userId) {
        redisTemplate.delete("refresh:" + userId);
    }

    public void blacklistAccessToken(String token, long ttl) {
        redisTemplate.opsForValue().set("blacklist:" + token, "true", ttl, TimeUnit.SECONDS);
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.toString().equals(redisTemplate.opsForValue().get("blacklist:" + token));
    }
}
