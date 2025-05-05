package org.example.order.core.infra.security.jwt.contract;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.security.Key;
import java.util.List;

/**
 * JWT 토큰 제공 인터페이스 (멀티 키 대응)
 */
public interface TokenProvider {

    boolean validateToken(String token);

    String getUserId(String token);

    List<SimpleGrantedAuthority> getRoles(String token);

    String getJti(String token);

    String getDevice(String token);

    String getIp(String token);

    String createAccessToken(String userId, List<String> roles, String jti,
                             String device, String ip, List<String> scopes);

    String createRefreshToken(String userId, String jti);

    /**
     * 현재 사용 중인 키 ID 반환
     */
    String getCurrentKid();

    /**
     * KeyResolver: 동적으로 검증 키를 제공 (HMAC/RSA 등 모두 대응)
     */
    @FunctionalInterface
    interface KeyResolver {
        Key resolveKey(String token);
    }

    /**
     * KidProvider: 현재 사용 중인 kid 반환 (JWT 헤더용)
     */
    @FunctionalInterface
    interface KidProvider {
        String getCurrentKid();
    }
}
