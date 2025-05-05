package org.example.order.core.infra.security.jwt.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.jwt.config.JwtConfigurationProperties;
import org.example.order.core.infra.security.jwt.constant.JwtClaimsConstants;
import org.example.order.core.infra.security.jwt.contract.TokenProvider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 토큰 매니저 추상 클래스
 * - 토큰 유효성 검사, 클레임 추출 공통 기능 제공
 */
@Slf4j
public abstract class AbstractJwtTokenManager implements TokenProvider {

    protected static final String LOG_PREFIX = "[JwtTokenManager]";

    protected final JwtConfigurationProperties jwtConfig;

    protected AbstractJwtTokenManager(JwtConfigurationProperties jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * 토큰 파싱 및 Claims 추출 (명시적 KeyResolver 사용)
     */
    public Claims getClaims(String token, TokenProvider.KeyResolver keyResolver) {
        return Jwts.parserBuilder()
                .setSigningKey(keyResolver.resolveKey(token))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰 파싱 및 Claims 추출 (내부 기본 KeyResolver 사용)
     */
    public Claims getClaims(String token) {
        return getClaims(token, this::resolveKeyForValidation);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            getClaims(token);

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("{} Invalid token: {}", LOG_PREFIX, e.getMessage());

            return false;
        }
    }

    @Override
    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<SimpleGrantedAuthority> getRoles(String token) {
        List<String> roles = (List<String>) getClaims(token).get(JwtClaimsConstants.ROLES);

        if (roles == null) {
            return List.of();
        }

        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getJti(String token) {
        return getClaims(token).getId();
    }

    @Override
    public String getDevice(String token) {
        return (String) getClaims(token).get(JwtClaimsConstants.DEVICE);
    }

    @Override
    public String getIp(String token) {
        return (String) getClaims(token).get(JwtClaimsConstants.IP);
    }

    /**
     * 하위 클래스가 구현해야 하는 키 리졸버
     */
    protected abstract java.security.Key resolveKeyForValidation(String token);
}
