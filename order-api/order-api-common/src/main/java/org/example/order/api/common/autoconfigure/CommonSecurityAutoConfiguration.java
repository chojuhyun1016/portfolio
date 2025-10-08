package org.example.order.api.common.autoconfigure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.example.order.api.common.infra.ApiInfraProperties;
import org.example.order.common.security.GatewayOnlyFilter;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.util.StringUtils;

/**
 * 공통 Spring Security 자동 구성(경량)
 * - Stateless, 접근패턴, GatewayOnlyFilter(옵션)
 * - CorrelationIdFilter 등록은 서블릿 자동구성에서만 처리(중복 방지)
 * - props(order.api.infra.security.*)만 사용
 */
@AutoConfiguration
@EnableConfigurationProperties(ApiInfraProperties.class)
@ConditionalOnClass({SecurityFilterChain.class, HttpSecurity.class})
@ConditionalOnProperty(
        prefix = "order.api.infra.security",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CommonSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<GatewayOnlyFilter> gatewayOnlyFilterProvider,
            ApiInfraProperties props
    ) throws Exception {

        final String[] permit = props.getSecurity() != null && props.getSecurity().permitAllPatterns() != null
                ? props.getSecurity().permitAllPatterns() : new String[0];
        final String[] auth = props.getSecurity() != null && props.getSecurity().authenticatedPatterns() != null
                ? props.getSecurity().authenticatedPatterns() : new String[0];

        http
                .csrf(c -> c.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> {
                    for (String p : permit) {
                        if (StringUtils.hasText(p)) {
                            reg.requestMatchers(p).permitAll();
                        }
                    }

                    for (String p : auth) {
                        if (StringUtils.hasText(p)) {
                            reg.requestMatchers(p).authenticated();
                        }
                    }
                    reg.anyRequest().authenticated();
                });

        GatewayOnlyFilter gwFilter = gatewayOnlyFilterProvider.getIfAvailable();

        if (gwFilter != null) {
            http.addFilterBefore(gwFilter, AnonymousAuthenticationFilter.class);
        }

        return http.build();
    }

    /**
     * GatewayOnlyFilter 빈은 게이트웨이 시크릿이 설정됐을 때만 생성.
     * - secret 이 비어 있으면 생성하지 않음(=등록 안 함)
     */
    @Bean
    @ConditionalOnMissingBean(GatewayOnlyFilter.class)
    public GatewayOnlyFilter gatewayOnlyFilter(ApiInfraProperties props) {
        if (props.getSecurity() == null || props.getSecurity().gateway() == null) {
            return null;
        }

        var gw = props.getSecurity().gateway();
        String header = gw.header();
        String secret = gw.secret();

        if (!StringUtils.hasText(secret)) {
            return null;
        }

        Set<String> white = new HashSet<>();
        String[] permitArr = props.getSecurity().permitAllPatterns();

        if (permitArr != null) {
            Arrays.stream(permitArr)
                    .filter(StringUtils::hasText)
                    .forEach(white::add);
        }

        if (!StringUtils.hasText(header)) {
            header = "X-Internal-Gateway";
        }

        return new GatewayOnlyFilter(header, secret, white);
    }
}
