package org.example.order.api.master.security;

import lombok.RequiredArgsConstructor;
import org.example.order.api.common.constants.ApiKeyConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.List;

/**
 * API Key 기반 인증 보안 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ApiKeySecurityConfig {

    private final ApiKeyAuthEntryPoint authEntryPoint;
    private final UserDetailsService userDetailsService;

    private static final List<String> PUBLIC_URLS = List.of(
            "/docs/**", "/favicon.ico", "/actuator/**", "/error"
    );

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS.toArray(new String[0])).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint))
                .addFilterBefore(apiKeyAuthFilter(authenticationManager()), AbstractPreAuthenticatedProcessingFilter.class);

        return http.build();
    }

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter(AuthenticationManager authenticationManager) {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(ApiKeyConstants.HEADER_NAME);
        filter.setAuthenticationManager(authenticationManager);

        return filter;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return authenticationProvider()::authenticate;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(preAuthenticatedUserDetailsService());

        return provider;
    }

    @Bean
    public AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> preAuthenticatedUserDetailsService() {
        return token -> userDetailsService.loadUserByUsername((String) token.getPrincipal());
    }
}