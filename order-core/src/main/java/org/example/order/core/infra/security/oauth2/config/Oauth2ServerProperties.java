package org.example.order.core.infra.security.oauth2.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Oauth2 서버 프로퍼티 설정 클래스
 * - 발급자(issuer), 만료시간, 서명 키 등을 관리
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "oauth2.server")
public class Oauth2ServerProperties {

    private String issuer;
    private int accessTokenValiditySeconds = 3600;       // 액세스 토큰 유효기간 (초)
    private int refreshTokenValiditySeconds = 1209600;   // 리프레시 토큰 유효기간 (초)
    private String signingKey;                            // 서명 키
}
