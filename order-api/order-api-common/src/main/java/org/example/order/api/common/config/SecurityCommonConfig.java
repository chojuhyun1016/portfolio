package org.example.order.api.common.config;

import java.util.HashSet;
import java.util.Set;

import org.example.order.api.common.properties.ApiInfraProperties;
import org.example.order.common.security.GatewayOnlyFilter;
import org.example.order.common.web.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * 공통 Security 설정:
 * - Stateless
 * - CorrelationIdFilter + GatewayOnlyFilter
 * - yml로 permitAll/authenticated 패턴 제어
 */
@AutoConfiguration
@EnableConfigurationProperties(ApiInfraProperties.class)
public class SecurityCommonConfig {

    @Bean
    @ConditionalOnProperty(prefix = "order.api.infra.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorrelationIdFilter correlationIdFilter,
            GatewayOnlyFilter gatewayOnlyFilter,
            ApiInfraProperties props
    ) throws Exception {

        var sec = props.getSecurity();

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // permitAll
                    for (String pattern : sec.permitAllPatterns()) {
                        auth.requestMatchers(pattern).permitAll();
                    }

                    // authenticated (GET/POST 등 세분화 원하면 확장)
                    for (String pattern : sec.authenticatedPatterns()) {
                        auth.requestMatchers(pattern).authenticated();
                    }

                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(correlationIdFilter, AnonymousAuthenticationFilter.class)
                .addFilterBefore(gatewayOnlyFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "order.api.infra.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @ConditionalOnProperty(prefix = "order.api.infra.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GatewayOnlyFilter gatewayOnlyFilter(ApiInfraProperties props) {
        var gw = props.getSecurity().gateway();
        Set<String> white = new HashSet<>(props.getSecurity().permitAllPatterns());

        return new GatewayOnlyFilter(gw.header(), gw.secret(), white);
    }
}
