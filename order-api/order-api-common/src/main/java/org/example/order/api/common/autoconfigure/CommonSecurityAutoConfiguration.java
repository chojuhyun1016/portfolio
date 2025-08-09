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
 * 공통 시큐리티 자동 구성
 * <p>
 * 목적
 * API 서비스에서 공통적으로 사용하는 SecurityFilterChain과 필터를 자동 구성
 * common 모듈을 포함시키면 즉시 보안 설정이 적용되며, 서비스별 재정의 가능
 * <p>
 * 주요 기능
 * 1. application.yml 에서 order.api.infra.security.enabled 가 true 일 때만 활성화
 * 2. Spring Security 를 Stateless 로 설정 (세션 미사용)
 * 3. application.yml 의 permitAllPatterns 와 authenticatedPatterns 기반 접근 제어
 * 4. CorrelationIdFilter 와 GatewayOnlyFilter 를 기본 필터로 등록
 * 5. SecurityFilterChain, CorrelationIdFilter, GatewayOnlyFilter 가 이미 빈으로 정의된 경우 공통 빈은 생성하지 않음
 * <p>
 * 재정의 방법
 * 서비스에서 SecurityFilterChain 타입의 Bean 을 등록하면 본 설정은 적용되지 않음
 * application.yml 에서 패턴, 게이트웨이 정보, 시큐리티 활성화 여부를 조정할 수 있음
 */
@AutoConfiguration
@EnableConfigurationProperties(ApiInfraProperties.class)
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnProperty(prefix = "order.api.infra.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CommonSecurityAutoConfiguration {

    /**
     * 기본 SecurityFilterChain 설정
     * CSRF 비활성화
     * 세션 사용 안 함
     * 패턴 기반 접근 제어
     * CorrelationIdFilter, GatewayOnlyFilter 를 AnonymousAuthenticationFilter 이전에 등록
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorrelationIdFilter correlationIdFilter,
            GatewayOnlyFilter gatewayOnlyFilter,
            ApiInfraProperties props
    ) throws Exception {
        var sec = props.getSecurity();

        http
                .csrf(c -> c.disable()) // CSRF 비활성화
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 사용 안 함
                .authorizeHttpRequests(auth -> {
                    for (String pattern : sec.permitAllPatterns()) {
                        auth.requestMatchers(pattern).permitAll();
                    }
                    for (String pattern : sec.authenticatedPatterns()) {
                        auth.requestMatchers(pattern).authenticated();
                    }
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(correlationIdFilter, AnonymousAuthenticationFilter.class)
                .addFilterBefore(gatewayOnlyFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CorrelationIdFilter 빈 등록
     * 요청 간 추적을 위해 Correlation ID 를 생성하고 전달하는 필터
     * 서비스에서 이미 빈을 정의하면 생성하지 않음
     */
    @Bean
    @ConditionalOnMissingBean(CorrelationIdFilter.class)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    /**
     * GatewayOnlyFilter 빈 등록
     * 지정된 게이트웨이 헤더와 시크릿을 검증하여 접근을 제어
     * permitAllPatterns 에 포함된 경로는 화이트리스트 처리
     * 서비스에서 이미 빈을 정의하면 생성하지 않음
     */
    @Bean
    @ConditionalOnMissingBean(GatewayOnlyFilter.class)
    public GatewayOnlyFilter gatewayOnlyFilter(ApiInfraProperties props) {
        var gw = props.getSecurity().gateway();

        String[] permitArr = props.getSecurity().permitAllPatterns();
        Set<String> white = new HashSet<>();
        if (permitArr != null) {
            Arrays.stream(permitArr)
                    .filter(s -> s != null && !s.isBlank())
                    .forEach(white::add);
        }

        return new GatewayOnlyFilter(gw.header(), gw.secret(), white);
    }
}
