package org.example.order.core.infra.security.jwt.provider;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.kms.decryptor.KmsDecryptor;
import org.example.order.core.infra.crypto.config.EncryptProperties;
import org.example.order.core.infra.security.jwt.config.JwtConfigurationProperties;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenManager {

    private final JwtConfigurationProperties jwtConfigurationProperties;
    private final KmsDecryptor kmsDecryptor;
    private final EncryptProperties encryptProperties;

    private Key secretKey;

    @PostConstruct
    public void init() {
        try {
            byte[] decrypted = kmsDecryptor.decryptBase64EncodedKey(encryptProperties.getAes128().getKey());
            byte[] decodedKey = Base64.getDecoder().decode(decrypted);
            this.secretKey = Keys.hmacShaKeyFor(decodedKey);
            log.info("[JWT] Secret key initialized (KMS + Base64)");
        } catch (Exception e) {
            log.error("[JWT] Secret key initialization failed", e);
            throw new IllegalStateException("Secret key setup failed", e);
        }
    }

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

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] Invalid token: {}", e.getMessage());
            return false;
        }
    }

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

    public Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }

    public String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        return (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
    }

    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    public List<SimpleGrantedAuthority> getRoles(String token) {
        List<String> roles = (List<String>) getClaims(token).get("roles");
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    public String getJti(String token) {
        return getClaims(token).getId();
    }

    public long getRemainingSeconds(String token) {
        return (getClaims(token).getExpiration().getTime() - System.currentTimeMillis()) / 1000;
    }

    public List<String> getScopes(String token) {
        return (List<String>) getClaims(token).get("scope");
    }

    public String getDevice(String token) {
        return (String) getClaims(token).get("device");
    }

    public String getIp(String token) {
        return (String) getClaims(token).get("ip");
    }

    public UserDetails getAuthentication(String token) {
        return new User(getUserId(token), "", getRoles(token));
    }
}
