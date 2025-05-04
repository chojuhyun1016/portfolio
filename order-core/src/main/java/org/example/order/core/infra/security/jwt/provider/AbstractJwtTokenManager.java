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

    // JWT 설정 프로퍼티 (만료 시간 등)
    protected final JwtConfigurationProperties jwtConfig;

    protected AbstractJwtTokenManager(JwtConfigurationProperties jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * 토큰 파싱 및 Claims 추출 (명시적 KeyResolver 사용)
     */
    public Claims getClaims(String token, KeyResolver keyResolver) {
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

    /**
     * 토큰 유효성 검사
     */
    @Override
    public boolean validateToken(String token) {
        try {
            getClaims(token);  // 파싱만 성공하면 유효

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("{} Invalid token: {}", LOG_PREFIX, e.getMessage());

            return false;
        }
    }

    /**
     * 토큰에서 사용자 ID(subject) 추출
     */
    @Override
    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 토큰에서 권한(roles) 추출 (SimpleGrantedAuthority로 변환)
     */
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

    /**
     * 토큰에서 JTI 추출
     */
    @Override
    public String getJti(String token) {
        return getClaims(token).getId();
    }

    /**
     * 토큰에서 디바이스 정보 추출
     */
    @Override
    public String getDevice(String token) {
        return (String) getClaims(token).get(JwtClaimsConstants.DEVICE);
    }

    /**
     * 토큰에서 IP 정보 추출
     */
    @Override
    public String getIp(String token) {
        return (String) getClaims(token).get(JwtClaimsConstants.IP);
    }

    /**
     * KeyResolver: 동적으로 키를 해석할 수 있도록 함수형 인터페이스 정의
     */
    @FunctionalInterface
    public interface KeyResolver {
        java.security.Key resolveKey(String token);
    }

    /**
     * 기본 키 리졸버: 하위 클래스에서 구현 필요
     */
    protected java.security.Key resolveKeyForValidation(String token) {
        throw new UnsupportedOperationException("Subclasses must override resolveKeyForValidation()");
    }
}
