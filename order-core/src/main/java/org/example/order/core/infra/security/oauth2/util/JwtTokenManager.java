package org.example.order.core.infra.security.oauth2.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;
import org.example.order.core.infra.security.oauth2.config.Oauth2ServerProperties;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * JWT 발급/검증 유틸리티.
 */
@Component
public class JwtTokenManager {

    private final String signingKey;

    public JwtTokenManager(Oauth2ServerProperties properties) {
        this.signingKey = properties.getSigningKey();
    }

    /**
     * JWT 토큰 생성 메서드.
     * @param subject 사용자 식별자 (예: userId)
     * @param claims  커스텀 클레임 맵
     * @param validitySeconds 토큰 유효 시간 (초)
     * @return 서명된 JWT 문자열
     */
    public String generateToken(String subject, Map<String, Object> claims, long validitySeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)                                 // 커스텀 클레임 설정
                .setSubject(subject)                               // 사용자 식별자 설정
                .setIssuedAt(Date.from(now))                        // 발급 시각
                .setExpiration(Date.from(now.plusSeconds(validitySeconds))) // 만료 시각
                .signWith(SignatureAlgorithm.HS256, signingKey)    // HMAC 서명
                .compact();
    }
}
