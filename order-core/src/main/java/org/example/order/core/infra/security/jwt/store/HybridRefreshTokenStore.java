package org.example.order.core.infra.security.jwt.store;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.domain.security.entity.RefreshTokenEntity;
import org.example.order.core.infra.jpa.repository.security.RefreshTokenRepository;
import org.example.order.core.infra.redis.repository.RefreshTokenRedisRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

/**
 * RefreshTokenStore 구현체 (Redis + DB Fallback)
 * Redis 캐시 우선 → DB Fallback → 필요 시 Redis 복구
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridRefreshTokenStore implements RefreshTokenStore {

    private final RefreshTokenRedisRepository redisRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Refresh 토큰 저장 (Redis + DB)
     *
     * @param userId     사용자 ID
     * @param token      토큰 값
     * @param ttlSeconds 만료 시간 (초)
     */
    @Override
    public void storeRefreshToken(String userId, String token, long ttlSeconds) {
        // Redis 저장
        redisRepository.save(userId, token, ttlSeconds, TimeUnit.SECONDS);

        // DB 저장
        LocalDateTime expiry = LocalDateTime.now().plusSeconds(ttlSeconds);
        RefreshTokenEntity entity = new RefreshTokenEntity(userId, token, expiry);
        refreshTokenRepository.save(entity);

        log.debug("[RefreshTokenStore] Stored refresh token - userId={}, expiry={}", userId, expiry);
    }

    /**
     * Refresh 토큰 유효성 검증 (Redis → DB fallback)
     *
     * @param userId 사용자 ID
     * @param token  전달받은 토큰
     * @return 유효성 여부
     */
    @Override
    public boolean validateRefreshToken(String userId, String token) {
        // 1차: Redis 조회
        Optional<String> redisTokenOpt = redisRepository.findByKey(userId);

        if (redisTokenOpt.isPresent()) {
            return token.equals(redisTokenOpt.get());
        }

        // 2차: DB fallback
        Optional<RefreshTokenEntity> entityOpt = refreshTokenRepository.findByUserId(userId);

        if (entityOpt.isEmpty()) {
            log.warn("[RefreshTokenStore] No refresh token found for userId={} (DB fallback)", userId);

            return false;
        }

        RefreshTokenEntity entity = entityOpt.get();

        // 만료 여부 확인
        if (isExpired(entity.getExpiryDatetime())) {
            log.warn("[RefreshTokenStore] Refresh token expired for userId={} (DB fallback)", userId);

            return false;
        }

        // Redis 복구 시도
        long ttlSeconds = Duration.between(LocalDateTime.now(), entity.getExpiryDatetime()).getSeconds();

        if (ttlSeconds > 0) {
            redisRepository.save(userId, entity.getToken(), ttlSeconds, TimeUnit.SECONDS);

            log.info("[RefreshTokenStore] Restored refresh token to Redis - userId={}, ttl={}s", userId, ttlSeconds);
        }

        return token.equals(entity.getToken());
    }

    /**
     * Refresh 토큰 제거 (Redis + DB 모두 삭제)
     *
     * @param userId 사용자 ID
     */
    @Override
    public void removeRefreshToken(String userId) {
        redisRepository.delete(userId);
        refreshTokenRepository.deleteByUserId(userId);

        log.info("[RefreshTokenStore] Removed refresh token - userId={}", userId);
    }

    /**
     * 블랙리스트 조회
     *
     * @param token 토큰 값
     * @return 블랙리스트 여부
     */
    @Override
    public boolean isBlacklisted(String token) {
        return redisRepository.isBlacklisted(token);
    }

    /**
     * 토큰을 블랙리스트에 등록
     *
     * @param token      토큰 값
     * @param ttlSeconds 블랙리스트 유지 시간 (초)
     */
    @Override
    public void blacklistToken(String token, long ttlSeconds) {
        redisRepository.blacklist(token, ttlSeconds, TimeUnit.SECONDS);
        log.info("[RefreshTokenStore] Blacklisted access token - ttl={}s", ttlSeconds);
    }

    /**
     * Redis 상 Refresh 토큰의 남은 TTL 조회
     *
     * @param userId 사용자 ID
     * @return 남은 TTL (없으면 -1)
     */
    @Override
    public long getRemainingTtl(String userId) {
        OptionalLong ttlOpt = redisRepository.getRemainingTtl(userId, TimeUnit.SECONDS);

        return ttlOpt.orElse(-1L);
    }

    /**
     * DB 청소 메서드: 만료된 Refresh 토큰을 주기적으로 삭제
     *
     * @return 삭제된 토큰 개수
     */
    public int cleanExpiredTokens() {
        List<RefreshTokenEntity> expiredTokens = refreshTokenRepository.findExpiredTokens();
        expiredTokens.forEach(entity -> refreshTokenRepository.deleteById(entity.getUserId()));
        log.info("[RefreshTokenStore] Cleaned {} expired refresh tokens from DB", expiredTokens.size());

        return expiredTokens.size();
    }

    /**
     * 토큰 만료 여부 판단
     *
     * @param expiry 만료 일시
     * @return 만료 여부
     */
    private boolean isExpired(LocalDateTime expiry) {
        return expiry.isBefore(LocalDateTime.now());
    }
}
