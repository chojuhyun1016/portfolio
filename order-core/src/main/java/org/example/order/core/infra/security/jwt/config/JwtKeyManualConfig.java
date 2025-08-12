package org.example.order.core.infra.security.jwt.config;

import org.example.order.core.infra.security.jwt.contract.TokenProvider;
import org.example.order.core.infra.security.jwt.resolver.JwtKeyProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 인메모리 JWT 키를 명시적으로 사용할 때만 켜지는 구성.
 * jwt.manual-key.enabled=true 일 때만 등록됨.
 */
@Configuration
@ConditionalOnProperty(name = "jwt.manual-key.enabled", havingValue = "true")
public class JwtKeyManualConfig {

    @Bean
    public JwtKeyProvider jwtKeyProvider() {
        // 빈으로만 등록 (키 값은 부팅 시 코드에서 setKey(...)로 주입하거나,
        // 별도 초기화 빈에서 주입)
        return new JwtKeyProvider().setKid("app-jwt-key");
    }

    @Bean
    public TokenProvider.KeyResolver jwtKeyResolver(JwtKeyProvider provider) {
        return provider;
    }

    @Bean
    public TokenProvider.KidProvider jwtKidProvider(JwtKeyProvider provider) {
        return provider;
    }
}
