package org.example.order.core.infra.security.jwt.constant;

/**
 * JWT 헤더 관련 상수 정의
 *
 * 이 클래스는 JWT 인증 시 사용되는 헤더 정보들을 상수로 정의합니다.
 * - Authorization 헤더의 키 이름과 Bearer 접두어를 관리합니다.
 *
 * 사용 예:
 * - HttpServletRequest.getHeader(JwtHeaderConstants.AUTHORIZATION)
 * - "Bearer " 접두어 체크 시 JwtHeaderConstants.BEARER_PREFIX 활용
 */
public final class JwtHeaderConstants {

    // 인스턴스화 방지
    private JwtHeaderConstants() {}

    /**
     * HTTP Authorization 헤더 키
     * 예: Authorization: Bearer <token>
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * Authorization 헤더의 Bearer 접두사
     * 예: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     */
    public static final String BEARER_PREFIX = "Bearer ";
}
