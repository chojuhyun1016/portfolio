package org.example.order.core.infra.security.jwt.provider;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public interface TokenProvider {

    boolean validateToken(String token);

    String getUserId(String token);

    List<SimpleGrantedAuthority> getRoles(String token);  // ✅ 변경됨

    String getJti(String token);

    String getDevice(String token);

    String getIp(String token);

    String createAccessToken(String userId, List<String> roles, String jti,
                             String device, String ip, List<String> scopes);

    String createRefreshToken(String userId, String jti);
}
