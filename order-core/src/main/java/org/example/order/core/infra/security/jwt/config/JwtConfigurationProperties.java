package org.example.order.core.infra.security.jwt.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 설정 프로퍼티
 * - 토큰 만료 시간 등 동작 파라미터만 관리
 * - 서명 키는 외부 KeyResolver(JwtKeyProvider 등)로 주입
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfigurationProperties {

    /**
     * AccessToken 유효시간 (초)
     */
    private long accessTokenValidityInSeconds;

    /**
     * RefreshToken 유효시간 (초)
     */
    private long refreshTokenValidityInSeconds;
}
