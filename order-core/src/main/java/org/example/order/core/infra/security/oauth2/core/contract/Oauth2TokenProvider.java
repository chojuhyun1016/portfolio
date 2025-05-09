package org.example.order.core.infra.security.oauth2.core.contract;

/**
 * Oauth2 토큰 발급 및 검증 인터페이스
 * - 구현 서비스 쪽에서 직접 DB/Redis 처리
 */
public interface Oauth2TokenProvider {

    /**
     * 액세스 토큰 생성
     *
     * @param request 요청 (구현 서비스 쪽에서 정의)
     * @return 액세스 토큰 (JSON 문자열 등)
     */
    Object createAccessToken(Object request);

    /**
     * 리프레시 토큰으로 액세스 토큰 재발급
     *
     * @param userId        사용자 ID
     * @param refreshToken  리프레시 토큰
     * @return 액세스 토큰 (JSON 문자열 등)
     */
    Object refreshAccessToken(String userId, String refreshToken);

    /**
     * 액세스 토큰 검증
     */
    boolean validateAccessToken(String token);

    /**
     * 리프레시 토큰 검증
     */
    boolean validateRefreshToken(String userId, String refreshToken);
}
