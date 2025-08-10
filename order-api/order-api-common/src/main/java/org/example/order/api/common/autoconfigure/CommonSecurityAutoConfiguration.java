package org.example.order.api.common.autoconfigure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.example.order.api.common.infra.ApiInfraProperties;
import org.example.order.common.security.GatewayOnlyFilter;
import org.example.order.common.web.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * 공통 Spring Security 자동 구성
 * - 기본 동작: CSRF 비활성화, Stateless 세션, URL 패턴 접근 제어, 공통 필터 추가
 * - 설정 누락 시에도 기본값으로 동작
 * - 서비스가 SecurityFilterChain 빈을 정의하면 본 구성은 비활성화
 */
@AutoConfiguration
@EnableConfigurationProperties(ApiInfraProperties.class)
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnProperty(prefix = "order.api.infra.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CommonSecurityAutoConfiguration {

    /**
     * 기본 SecurityFilterChain
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorrelationIdFilter correlationIdFilter,
            GatewayOnlyFilter gatewayOnlyFilter,
            ApiInfraProperties props
    ) throws Exception {

        final String[] permit = props.getSecurity().permitAllPatterns() == null
                ? new String[0] : props.getSecurity().permitAllPatterns();
        final String[] auth = props.getSecurity().authenticatedPatterns() == null
                ? new String[0] : props.getSecurity().authenticatedPatterns();

        http
                .csrf(c -> c.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> {
                    for (String p : permit) if (p != null && !p.isBlank()) reg.requestMatchers(p).permitAll();
                    for (String p : auth) if (p != null && !p.isBlank()) reg.requestMatchers(p).authenticated();
                    reg.anyRequest().authenticated();
                })
                .addFilterBefore(correlationIdFilter, AnonymousAuthenticationFilter.class)
                .addFilterBefore(gatewayOnlyFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CorrelationIdFilter 기본 빈
     */
    @Bean
    @ConditionalOnMissingBean(CorrelationIdFilter.class)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    /**
     * GatewayOnlyFilter 기본 빈
     */
    @Bean
    @ConditionalOnMissingBean(GatewayOnlyFilter.class)
    public GatewayOnlyFilter gatewayOnlyFilter(ApiInfraProperties props) {
        var gw = props.getSecurity().gateway();

        Set<String> white = new HashSet<>();
        String[] permitArr = props.getSecurity().permitAllPatterns();
        if (permitArr != null) {
            Arrays.stream(permitArr)
                    .filter(s -> s != null && !s.isBlank())
                    .forEach(white::add);
        }
        return new GatewayOnlyFilter(gw.header(), gw.secret(), white);
    }
}
