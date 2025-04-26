package org.example.order.core.infra.redis.service;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.redis.repository.RedisRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * RefreshToken을 Redis에 저장 및 TTL 관리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenStoreService {

    private final RedisRepository redisRepository;

    private static final String REFRESH_PREFIX = "refresh_token:";
    private static final long DEFAULT_REFRESH_TOKEN_LIFESPAN_SECONDS = TimeUnit.DAYS.toSeconds(7); // 기본 RefreshToken 수명: 7일
    private static final long DEFAULT_REISSUE_THRESHOLD_SECONDS = TimeUnit.MINUTES.toSeconds(30); // 기본 재발급 임계: 30분

    /**
     * RefreshToken 저장 (최초 발급)
     */
    public void saveRefreshToken(String userId, String token) {
        redisRepository.set(REFRESH_PREFIX + userId, token, DEFAULT_REFRESH_TOKEN_LIFESPAN_SECONDS);
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

    /**
     * RefreshToken 재발급 필요 여부 확인 (임계값 직접 주입 가능)
     * @param userId 사용자 ID
     * @param thresholdSeconds 재발급 기준 시간(초). 예) 1800초(30분)
     * @return true = 재발급 필요, false = 연장 불필요
     */
    public boolean shouldReissueRefreshToken(String userId, long thresholdSeconds) {
        Long remainingSeconds = getRefreshTokenRemainingSeconds(userId);
        return remainingSeconds != null && remainingSeconds <= thresholdSeconds;
    }

    /**
     * RefreshToken 재발급 필요 여부 확인 (기본 30분 임계 사용)
     */
    public boolean shouldReissueRefreshToken(String userId) {
        return shouldReissueRefreshToken(userId, DEFAULT_REISSUE_THRESHOLD_SECONDS);
    }
}
