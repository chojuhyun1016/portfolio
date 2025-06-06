package org.example.order.core.infra.security.jwt.provider;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.jwt.config.JwtConfigurationProperties;
import org.example.order.core.infra.security.jwt.constant.JwtClaimsConstants;
import org.example.order.core.infra.security.jwt.contract.TokenProvider;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * SecretsKeyResolverAdapter 기반 JWT 토큰 매니저.
 * 실시간 키 핫스왑을 활용.
 */
@Slf4j
@Component
public class JwtTokenManager extends AbstractJwtTokenManager {

    private final TokenProvider.KeyResolver keyResolver;
    private final TokenProvider.KidProvider kidProvider;

    public JwtTokenManager(JwtConfigurationProperties jwtConfig,
                           TokenProvider.KeyResolver keyResolver,
                           TokenProvider.KidProvider kidProvider) {
        super(jwtConfig);
        this.keyResolver = keyResolver;
        this.kidProvider = kidProvider;
    }

    @Override
    public String createAccessToken(String userId, List<String> roles, String jti,
                                    String device, String ip, List<String> scopes) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getAccessTokenValidityInSeconds() * 1000);

        return Jwts.builder()
                .setHeaderParam("kid", kidProvider.getCurrentKid())
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setId(jti)
                .claim(JwtClaimsConstants.ROLES, roles)
                .claim(JwtClaimsConstants.SCOPE, scopes)
                .claim(JwtClaimsConstants.DEVICE, device)
                .claim(JwtClaimsConstants.IP, ip)
                .signWith(keyResolver.resolveKey(null), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String createRefreshToken(String userId, String jti) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getRefreshTokenValidityInSeconds() * 1000);

        return Jwts.builder()
                .setHeaderParam("kid", kidProvider.getCurrentKid())
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setId(jti)
                .claim(JwtClaimsConstants.ROLES, List.of("ROLE_REFRESH"))
                .signWith(keyResolver.resolveKey(null), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String getCurrentKid() {
        return kidProvider.getCurrentKid();
    }

    @Override
    protected java.security.Key resolveKeyForValidation(String token) {
        return keyResolver.resolveKey(token);
    }
}
