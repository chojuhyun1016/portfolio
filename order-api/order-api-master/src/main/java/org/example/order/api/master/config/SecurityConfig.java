package org.example.order.api.master.config;

import org.example.order.api.common.constant.AuthConstant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final List<String> ALLOWED_URLS = List.of("/docs/**", "/favicon.ico", "/actuator/**", "/error");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiKeyAuthFilter apiKeyAuthFilter, AuthenticationManager authManager, ApiKeyAuthEntryPoint entryPoint) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(ALLOWED_URLS.toArray(String[]::new)).permitAll()
                            .anyRequest().authenticated();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(apiKeyAuthFilter, AbstractPreAuthenticatedProcessingFilter.class)
                .authenticationManager(authManager)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(entryPoint)
                );

        return http.build();
    }

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter(AuthenticationManager authManager) {
        ApiKeyAuthFilter apiKeyAuthFilter = new ApiKeyAuthFilter(AuthConstant.X_API_KEY);
        apiKeyAuthFilter.setAuthenticationManager(authManager);
        return apiKeyAuthFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider apiKeyAuthProvider) {
        return apiKeyAuthProvider::authenticate;
    }

    @Bean
    public AuthenticationProvider apiKeyAuthProvider(AuthenticationUserDetailsService<?> apiKeyAuthValidator) {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService((AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken>) apiKeyAuthValidator);
        return provider;
    }

    @Bean
    public AuthenticationUserDetailsService<?> apiKeyAuthValidator(UserDetailsService userDetailsService) {
        return token -> userDetailsService.loadUserByUsername((String) token.getPrincipal());
    }
}
