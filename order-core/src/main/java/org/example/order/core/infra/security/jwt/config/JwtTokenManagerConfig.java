package org.example.order.core.infra.security.jwt.config;

import org.example.order.core.infra.security.jwt.contract.TokenProvider;
import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JwtTokenManager 조건부 구성.
 * - jwt.enabled=true 이고
 * - TokenProvider.KeyResolver / KidProvider 가 빈으로 존재할 때만 등록
 * (운영에서만 켜고, 테스트/문서화 컨텍스트에선 자동 배제)
 */
@Configuration
@EnableConfigurationProperties(JwtConfigurationProperties.class)
@ConditionalOnProperty(name = "jwt.enabled", havingValue = "true")
@ConditionalOnBean({TokenProvider.KeyResolver.class, TokenProvider.KidProvider.class})
public class JwtTokenManagerConfig {

    @Bean
    public JwtTokenManager jwtTokenManager(
            JwtConfigurationProperties props,
            TokenProvider.KeyResolver keyResolver,
            TokenProvider.KidProvider kidProvider
    ) {
        return new JwtTokenManager(props, keyResolver, kidProvider);
    }
}
