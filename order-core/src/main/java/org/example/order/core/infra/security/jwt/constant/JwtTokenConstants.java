package org.example.order.core.infra.security.jwt.constant;

/**
 * JWT 관련 상수 정의
 */
public final class JwtTokenConstants {

    private JwtTokenConstants() {}

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_SCOPE = "scope";
    public static final String CLAIM_DEVICE = "device";
    public static final String CLAIM_IP = "ip";

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    // 에러 메시지
    public static final String INVALID_JTI = "Invalid JTI (reused or expired)";
    public static final String BLACKLISTED_TOKEN = "Token is blacklisted";
    public static final String INVALID_TOKEN = "Invalid or expired token";
}
