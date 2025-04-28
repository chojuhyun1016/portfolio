package org.example.order.core.infra.security.auth.config;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.security.auth.store.AuthTokenStoreService;
import org.example.order.core.infra.security.jwt.filter.JwtSecurityAuthenticationFilter;
import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT 인증 기반 Security Filter 설정
 */
@Configuration
@RequiredArgsConstructor
public class AuthSecurityConfig {

    private final JwtTokenManager jwtTokenManager;
    private final AuthTokenStoreService authTokenStoreService;

    @Bean
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",   // 로그인, 토큰 발급
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtSecurityAuthenticationFilter(jwtTokenManager, authTokenStoreService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
