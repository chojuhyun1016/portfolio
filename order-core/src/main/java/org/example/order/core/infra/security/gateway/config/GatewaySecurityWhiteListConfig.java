package org.example.order.core.infra.security.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 인증 없이 접근 가능한 경로 설정
 */
@Configuration
public class GatewaySecurityWhiteListConfig {

    @Bean
    public List<String> whiteList() {
        return List.of(
                "/api/v1/auth/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/actuator/health"
        );
    }
}
