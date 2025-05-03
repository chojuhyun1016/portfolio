package org.example.order.core.infra.security.jwt.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.jwt.config.JwtConfigurationProperties;
import org.example.order.core.infra.security.jwt.constant.JwtTokenConstants;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 토큰 매니저 추상 클래스 (토큰 유효성 검사 / 클레임 추출)
 */
@Slf4j
public abstract class AbstractJwtTokenManager implements TokenProvider {

    protected static final String LOG_PREFIX = "[JwtTokenManager]";
    protected final JwtConfigurationProperties jwtConfig;

    protected AbstractJwtTokenManager(JwtConfigurationProperties jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * 기존: KeyResolver 명시적 필요
     */
    public Claims getClaims(String token, KeyResolver keyResolver) {
        return Jwts.parserBuilder()
                .setSigningKey(keyResolver.resolveKey(token))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * ✅ 추가: 기본 키 리졸버를 이용한 간편 메서드
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
        List<String> roles = (List<String>) getClaims(token)
                .get(JwtTokenConstants.CLAIM_ROLES);

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
        return (String) getClaims(token).get(JwtTokenConstants.CLAIM_DEVICE);
    }

    @Override
    public String getIp(String token) {
        return (String) getClaims(token).get(JwtTokenConstants.CLAIM_IP);
    }

    @FunctionalInterface
    public interface KeyResolver {
        java.security.Key resolveKey(String token);
    }

    protected java.security.Key resolveKeyForValidation(String token) {
        throw new UnsupportedOperationException("Subclasses must override resolveKeyForValidation()");
    }
}
