package org.example.order.core.infra.security.jwt.constant;

/**
 * JWT 클레임 관련 상수 정의
 * - 각 상수는 JWT Claims에 포함되는 키 이름을 나타냅니다.
 */
public final class JwtClaimsConstants {

    private JwtClaimsConstants() {
        // 인스턴스화 방지
    }

    /**
     * 사용자의 권한(역할) 정보 클레임 키
     */
    public static final String ROLES = "roles";

    /**
     * 인증 범위(scope) 클레임 키
     */
    public static final String SCOPE = "scope";

    /**
     * 디바이스 정보 클레임 키
     */
    public static final String DEVICE = "device";

    /**
     * IP 주소 클레임 키
     */
    public static final String IP = "ip";
}
