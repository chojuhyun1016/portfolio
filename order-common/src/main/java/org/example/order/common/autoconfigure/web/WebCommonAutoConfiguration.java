package org.example.order.common.autoconfigure.web;

import org.example.order.common.web.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * WebCommonAutoConfiguration
 * ------------------------------------------------------------------------
 * 목적
 * - CorrelationIdFilter 자동 등록: 요청 단위 상관관계 ID(requestId/traceId) 브리지 제공.
 * - 환경에 맞춰 order 조정 가능.
 */
@AutoConfiguration
public class WebCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
        FilterRegistrationBean<CorrelationIdFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new CorrelationIdFilter());
        reg.setOrder(Integer.MIN_VALUE + 100);
        reg.addUrlPatterns("/*");

        return reg;
    }
}
