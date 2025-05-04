package org.example.order.core.infra.security.jwt.provider;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.security.jwt.config.JwtConfigurationProperties;
import org.example.order.core.infra.security.jwt.constant.JwtClaimsConstants;
import org.example.order.core.infra.security.jwt.contract.TokenProvider;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

/**
 * JWT 토큰 매니저 구현체
 * - AWS SecretsManager 연동을 통해 동적으로 키를 가져와 토큰을 생성/검증
 */
@Slf4j
@Component
public class JwtTokenManager extends AbstractJwtTokenManager implements TokenProvider {

    private final SecretsKeyResolver secretsKeyResolver;

    /**
     * SecretsManager 내 키 이름 (필요 시 프로퍼티화 가능)
     */
    private final String keyName = "jwt-signing-key";

    /**
     * 생성자
     *
     * @param jwtConfig             JWT 설정 프로퍼티
     * @param secretsKeyResolver    SecretsManager 연동 키 리졸버
     */
    public JwtTokenManager(JwtConfigurationProperties jwtConfig, SecretsKeyResolver secretsKeyResolver) {
        super(jwtConfig);
        this.secretsKeyResolver = secretsKeyResolver;
    }

    /**
     * 현재 SecretsManager에서 가져온 HMAC 서명 키 반환
     *
     * @return 서명용 키
     */
    private Key getCurrentSigningKey() {
        byte[] keyBytes = secretsKeyResolver.getCurrentKey(keyName);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 액세스 토큰 생성
     *
     * @param userId 사용자 ID
     * @param roles 권한 목록
     * @param jti JWT ID (고유 식별자)
     * @param device 디바이스 정보
     * @param ip 요청 IP
     * @param scopes 스코프 정보
     * @return 생성된 액세스 토큰 문자열
     */
    @Override
    public String createAccessToken(String userId, List<String> roles, String jti,
                                    String device, String ip, List<String> scopes) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getAccessTokenValidityInSeconds() * 1000);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setId(jti)
                .claim(JwtClaimsConstants.ROLES, roles)
                .claim(JwtClaimsConstants.SCOPE, scopes)
                .claim(JwtClaimsConstants.DEVICE, device)
                .claim(JwtClaimsConstants.IP, ip)
                .signWith(getCurrentSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 리프레시 토큰 생성
     *
     * @param userId 사용자 ID
     * @param jti JWT ID (고유 식별자)
     * @return 생성된 리프레시 토큰 문자열
     */
    @Override
    public String createRefreshToken(String userId, String jti) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getRefreshTokenValidityInSeconds() * 1000);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setId(jti)
                .claim(JwtClaimsConstants.ROLES, List.of("ROLE_REFRESH"))
                .signWith(getCurrentSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * AbstractJwtTokenManager에서 토큰 검증 시 호출되는 키 해석 메서드 구현
     *
     * @param token 토큰 문자열
     * @return 토큰 검증용 서명 키
     */
    @Override
    protected Key resolveKeyForValidation(String token) {
        return getCurrentSigningKey();
    }
}
