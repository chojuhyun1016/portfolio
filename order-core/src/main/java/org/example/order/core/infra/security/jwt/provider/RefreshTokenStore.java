package org.example.order.core.infra.security.jwt.provider;

/**
 * Refresh Token 스토어 인터페이스
 * - Redis 또는 DB 기반 구현체 가능
 */
public interface RefreshTokenStore {

    /**
     * Refresh 토큰 저장 (Redis or DB)
     *
     * @param userId     사용자 ID (또는 고유 JTI)
     * @param token      실제 Refresh 토큰
     * @param ttlSeconds 유효기간 (초)
     */
    void storeRefreshToken(String userId, String token, long ttlSeconds);

    /**
     * Refresh 토큰 유효성 검증
     *
     * @param userId 사용자 ID (또는 고유 JTI)
     * @param token  비교할 Refresh 토큰
     * @return 유효하면 true
     */
    boolean validateRefreshToken(String userId, String token);

    /**
     * Refresh 토큰 제거
     *
     * @param userId 사용자 ID (또는 고유 JTI)
     */
    void removeRefreshToken(String userId);

    /**
     * 토큰이 블랙리스트에 올라가 있는지 확인
     *
     * @param token 토큰
     * @return 블랙리스트 여부
     */
    boolean isBlacklisted(String token);

    /**
     * 토큰을 블랙리스트에 추가
     *
     * @param token      토큰
     * @param ttlSeconds 블랙리스트 유지 시간 (초)
     */
    void blacklistToken(String token, long ttlSeconds);

    /**
     * 현재 토큰의 남은 TTL (초)
     *
     * @param userId 사용자 ID (또는 고유 JTI)
     * @return 남은 TTL (없으면 -1)
     */
    long getRemainingTtl(String userId);
}
