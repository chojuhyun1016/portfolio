package org.example.order.core.infra.security.jwt.contract;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

/**
 * JWT 토큰 제공 인터페이스
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
}
