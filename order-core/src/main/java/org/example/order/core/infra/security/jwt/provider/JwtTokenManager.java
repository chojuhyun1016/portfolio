package org.example.order.core.infra.security.jwt.provider;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.security.jwt.config.JwtConfigurationProperties;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;

/**
 * JWT 토큰 생성, 검증, Claims 처리 매니저
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenManager {

    private final JwtConfigurationProperties jwtConfigurationProperties;
    private final SecretsKeyResolver secretsKeyResolver;

    private Key secretKey;

    /**
     * 애플리케이션 부팅 시 최초 Key 초기화
     */
    @PostConstruct
    public void init() {
        refreshSecretKey();
    }

    /**
     * Secrets Manager로부터 최신 키를 가져와 Key 객체로 초기화
     */
    private void refreshSecretKey() {
        byte[] keyBytes = secretsKeyResolver.getCurrentKey();
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("[JWT] Secret key initialized or refreshed from Secrets Manager.");
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(String userId, List<String> roles, String jti, String device, String aud, List<String> scopes, String ip) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfigurationProperties.getAccessTokenValidityInSeconds() * 1000);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setId(jti)
                .setAudience(aud)
                .claim("roles", roles)
                .claim("device", device)
                .claim("scope", scopes)
                .claim("ip", ip)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(String userId, String jti) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfigurationProperties.getRefreshTokenValidityInSeconds() * 1000);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setId(jti)
                .claim("roles", List.of("ROLE_REFRESH"))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * JWT 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] Invalid token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * IP, Scope, Device 추가 검증
     */
    public boolean validateClaims(String token, String requestIp, List<String> requiredScopes, List<String> allowedDevices) {
        Claims claims = getClaims(token);

        String tokenIp = (String) claims.get("ip");
        List<String> scopes = (List<String>) claims.get("scope");
        String device = (String) claims.get("device");

        if (tokenIp == null || !tokenIp.equals(requestIp)) {
            log.warn("[JWT] IP mismatch. requestIp={}, tokenIp={}", requestIp, tokenIp);
            return false;
        }

        if (scopes == null || scopes.stream().noneMatch(requiredScopes::contains)) {
            log.warn("[JWT] Required scope missing. tokenScopes={}", scopes);
            return false;
        }

        if (!allowedDevices.contains(device)) {
            log.warn("[JWT] Invalid device. device={}", device);
            return false;
        }

        return true;
    }

    /**
     * JWT Claims 조회
     */
    public Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }

    /**
     * HttpServletRequest로부터 토큰 추출
     */
    public String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        return (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
    }

    /**
     * 토큰으로부터 UserId 추출
     */
    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 토큰으로부터 권한 목록 추출
     */
    public List<SimpleGrantedAuthority> getRoles(String token) {
        List<String> roles = (List<String>) getClaims(token).get("roles");
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    /**
     * 토큰으로부터 JTI (토큰 고유 ID) 추출
     */
    public String getJti(String token) {
        return getClaims(token).getId();
    }

    /**
     * 토큰의 남은 유효 시간(초) 조회
     */
    public long getRemainingSeconds(String token) {
        return (getClaims(token).getExpiration().getTime() - System.currentTimeMillis()) / 1000;
    }

    /**
     * 토큰으로부터 Scope 목록 추출
     */
    public List<String> getScopes(String token) {
        return (List<String>) getClaims(token).get("scope");
    }

    /**
     * 토큰으로부터 Device 정보 추출
     */
    public String getDevice(String token) {
        return (String) getClaims(token).get("device");
    }

    /**
     * 토큰으로부터 IP 정보 추출
     */
    public String getIp(String token) {
        return (String) getClaims(token).get("ip");
    }

    /**
     * 토큰을 기반으로 UserDetails 생성
     */
    public UserDetails getAuthentication(String token) {
        return new User(getUserId(token), "", getRoles(token));
    }
}
