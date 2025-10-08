package org.example.order.common.autoconfigure.web;

import org.example.order.common.web.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * WebAutoConfiguration
 * ------------------------------------------------------------------------
 * 목적
 * - CorrelationIdFilter 자동 등록: 요청 단위 상관관계 ID(requestId/traceId) 브리지 제공.
 * - 환경에 맞춰 order 조정 가능.
 * <p>
 * (추가)
 * - 타입 기반 조건부 등록으로 중복 방지(@ConditionalOnMissingBean(CorrelationIdFilter.class)).
 * - 필터 인스턴스 빈과 레지스트레이션 빈을 분리해 재사용/확장 용이.
 * - 실행 순서 최상단(Ordered.HIGHEST_PRECEDENCE)로 명시.
 */
@AutoConfiguration
public class WebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CorrelationIdFilter.class)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean(name = "correlationIdFilterRegistration")
    @ConditionalOnMissingBean(name = "correlationIdFilterRegistration")
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter filter) {
        FilterRegistrationBean<CorrelationIdFilter> reg = new FilterRegistrationBean<>();
        reg.setName("correlationIdFilterRegistration");
        reg.setFilter(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");

        return reg;
    }
}
