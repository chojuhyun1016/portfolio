package org.example.order.core.infra.security.jwt.constant;

/**
 * JWT 에러 메시지 상수 정의 클래스.
 * 이 클래스는 토큰 검증 중 발생할 수 있는 다양한 오류 메시지를 상수로 제공합니다.
 */
public final class JwtErrorConstants {

    // private 생성자: 인스턴스화 방지
    private JwtErrorConstants() {}

    /** JTI 불일치 또는 만료된 경우 */
    public static final String INVALID_JTI = "Invalid JTI (reused or expired)";

    /** 블랙리스트에 올라간 토큰 */
    public static final String BLACKLISTED_TOKEN = "Token is blacklisted";

    /** 잘못되었거나 만료된 토큰 */
    public static final String INVALID_TOKEN = "Invalid or expired token";
}
