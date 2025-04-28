package org.example.order.core.infra.security.auth.store;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.redis.repository.RedisRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * RefreshToken 저장 및 TTL 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenStoreService {

    private static final String REFRESH_PREFIX = "refresh_token:";
    private static final long DEFAULT_REFRESH_TTL = TimeUnit.DAYS.toSeconds(7); // 기본 7일

    private final RedisRepository redisRepository;

    /**
     * RefreshToken 저장 (기본 TTL 사용)
     */
    public void saveRefreshToken(String userId, String token) {
        redisRepository.set(REFRESH_PREFIX + userId, token, DEFAULT_REFRESH_TTL);
    }

    /**
     * RefreshToken 저장 (커스텀 TTL)
     */
    public void saveRefreshToken(String userId, String token, long ttlSeconds) {
        redisRepository.set(REFRESH_PREFIX + userId, token, ttlSeconds);
    }

    /**
     * RefreshToken 조회
     */
    public String getRefreshToken(String userId) {
        return (String) redisRepository.get(REFRESH_PREFIX + userId);
    }

    /**
     * RefreshToken 삭제
     */
    public void deleteRefreshToken(String userId) {
        redisRepository.delete(REFRESH_PREFIX + userId);
    }

    /**
     * RefreshToken 남은 TTL 조회
     */
    public Long getRefreshTokenRemainingSeconds(String userId) {
        return redisRepository.getExpire(REFRESH_PREFIX + userId);
    }
}
