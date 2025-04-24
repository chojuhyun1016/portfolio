// 📄 File: org.example.order.core.infra.security.jwt.JwtTokenProvider.java

package org.example.order.core.infra.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.kms.decryptor.KmsDecryptor;
import org.example.order.core.infra.crypto.config.EncryptProperties;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final KmsDecryptor kmsDecryptor;
    private final EncryptProperties encryptProperties;

    private Key secretKey;

    @PostConstruct
    public void init() {
        try {
            // KMS 복호화 → Base64 디코딩 → HMAC 키 생성
            byte[] decrypted = kmsDecryptor.decryptBase64EncodedKey(encryptProperties.getAes128().getKey());
            byte[] decodedKey = Base64.getDecoder().decode(decrypted);
            this.secretKey = Keys.hmacShaKeyFor(decodedKey);

            log.info("[JWT] Secret key 초기화 완료 (KMS + Base64)");

        } catch (IllegalArgumentException e) {
            log.error("[JWT] 시크릿 키 디코딩 실패: KMS 복호화 또는 base64 인코딩 문제 확인 필요");

            throw new RuntimeException("Invalid JWT secret key", e);
        }
    }

    public String createAccessToken(String userId) {
        return createToken(userId, jwtProperties.getAccessTokenValidityInSeconds());
    }

    public String createRefreshToken(String userId) {
        return createToken(userId, jwtProperties.getRefreshTokenValidityInSeconds());
    }

    private String createToken(String subject, long validityInSeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInSeconds * 1000);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);

            return true;
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] 만료된 토큰입니다. exp={}", e.getClaims().getExpiration());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] 유효하지 않은 토큰입니다. message={}", e.getMessage());
        }

        return false;
    }

    public UserDetails getAuthentication(String token) {
        String userId = getUserIdFromToken(token);

        return new User(userId, "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public String getUserIdFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");

        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        return null;
    }

    public long getRefreshValiditySeconds() {
        return jwtProperties.getRefreshTokenValidityInSeconds();
    }

    public long getTokenRemainingSeconds(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            long now = System.currentTimeMillis();
            long exp = claims.getExpiration().getTime();
            return (exp - now) / 1000;
        } catch (Exception e) {
            log.warn("[JWT] 남은 TTL 계산 실패: {}", e.getMessage());
            return 0L;
        }
    }
}
