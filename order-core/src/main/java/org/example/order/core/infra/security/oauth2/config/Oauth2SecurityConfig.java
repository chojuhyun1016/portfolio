package org.example.order.core.infra.security.oauth2.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Oauth2 설정 클래스 (ConfigProperties 바인딩만 유지)
 */
@Configuration
@EnableConfigurationProperties({
        Oauth2ClientProperties.class,
        Oauth2ServerProperties.class
})
public class Oauth2SecurityConfig {
}
