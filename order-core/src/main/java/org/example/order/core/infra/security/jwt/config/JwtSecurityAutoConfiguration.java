package org.example.order.core.infra.security.jwt.config;

import org.example.order.core.infra.security.jwt.contract.TokenProvider;
import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 자동 구성
 * - 기본 OFF (jwt.enabled=true 일 때만 활성화)
 * - KeyResolver/KidProvider 빈이 존재해야 JwtTokenManager를 생성
 */
@Configuration
@EnableConfigurationProperties(JwtConfigurationProperties.class)
@ConditionalOnProperty(name = "jwt.enabled", havingValue = "true")
public class JwtSecurityAutoConfiguration {

    @Bean
    @ConditionalOnBean({TokenProvider.KeyResolver.class, TokenProvider.KidProvider.class})
    public JwtTokenManager jwtTokenManager(JwtConfigurationProperties props,
                                           TokenProvider.KeyResolver resolver,
                                           TokenProvider.KidProvider kidProvider) {
        return new JwtTokenManager(props, resolver, kidProvider);
    }
}
