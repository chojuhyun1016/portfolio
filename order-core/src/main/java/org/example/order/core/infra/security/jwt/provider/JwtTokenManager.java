package org.example.order.core.infra.security.jwt.provider;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.security.jwt.config.JwtConfigurationProperties;
import org.example.order.core.infra.security.jwt.constant.JwtTokenConstants;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

/**
 * JWT 토큰 매니저 구현체 - AWS SecretsManager 연동 (SecretsKeyResolver 통해 핫스왑 키 관리)
 */
@Slf4j
@Component
public class JwtTokenManager extends AbstractJwtTokenManager implements TokenProvider {

    private final SecretsKeyResolver secretsKeyResolver;
    private final String keyName = "jwt-signing-key";  // 🔑 JSON 내 키 이름 (필요시 프로퍼티화 권장)

    public JwtTokenManager(JwtConfigurationProperties jwtConfig, SecretsKeyResolver secretsKeyResolver) {
        super(jwtConfig);
        this.secretsKeyResolver = secretsKeyResolver;
    }

    /**
     * 현재 SecretsManager에서 가져온 키 반환
     */
    private Key getCurrentSigningKey() {
        byte[] keyBytes = secretsKeyResolver.getCurrentKey(keyName);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 액세스 토큰 생성
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
                .claim(JwtTokenConstants.CLAIM_ROLES, roles)
                .claim(JwtTokenConstants.CLAIM_SCOPE, scopes)
                .claim(JwtTokenConstants.CLAIM_DEVICE, device)
                .claim(JwtTokenConstants.CLAIM_IP, ip)
                .signWith(getCurrentSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 리프레시 토큰 생성
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
                .claim(JwtTokenConstants.CLAIM_ROLES, List.of("ROLE_REFRESH"))
                .signWith(getCurrentSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * ✅ AbstractJwtTokenManager에서 토큰 검증 시 호출되는 키 해석 메서드 구현
     */
    @Override
    protected Key resolveKeyForValidation(String token) {
        return getCurrentSigningKey();
    }
}
