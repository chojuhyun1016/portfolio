package org.example.order.core.infra.security.oauth2.config;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.security.oauth2.filter.Oauth2AuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Oauth2 보안 설정 클래스
 * - SecurityFilterChain 등록 및 필터 적용
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({Oauth2ClientProperties.class, Oauth2ServerProperties.class})
public class Oauth2SecurityConfig {

    private final Oauth2AuthenticationFilter oauth2AuthenticationFilter;

    @Bean
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(oauth2AuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
