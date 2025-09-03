package org.example.order.api.common.autoconfigure;

import org.example.order.api.common.autoconfigure.condition.LoggingConfigPresentCondition;
import org.example.order.api.common.infra.ApiInfraProperties;
import org.example.order.api.common.logging.MdcCorrelationFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * 공통 로깅 자동 구성
 * - 요청별 MDC 상관관계 아이디 필터 등록
 * - 설정 존재 시에만 활성(opt-in)
 */
@AutoConfiguration
@EnableConfigurationProperties(ApiInfraProperties.class)
@Conditional(LoggingConfigPresentCondition.class)
public class CommonLoggingAutoConfiguration {

    @Bean(name = "mdcCorrelationFilterRegistration")
    @ConditionalOnMissingBean(name = "mdcCorrelationFilterRegistration")
    public FilterRegistrationBean<MdcCorrelationFilter> mdcCorrelationFilterRegistration(ApiInfraProperties props) {
        FilterRegistrationBean<MdcCorrelationFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new MdcCorrelationFilter(props));
        reg.setOrder(props.getLogging().getMdcFilterOrder());
        reg.addUrlPatterns("/*");

        return reg;
    }
}
