package org.example.order.api.common.config;

import java.util.HashSet;

import org.example.order.api.common.logging.RequestLoggingFilter;
import org.example.order.api.common.properties.ApiInfraProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * 공통 로깅 필터 등록(@Servlet 필터)
 */
@AutoConfiguration
@EnableConfigurationProperties(ApiInfraProperties.class)
public class LoggingCommonConfig {

    @Bean
    @ConditionalOnProperty(prefix = "order.api.infra.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter(ApiInfraProperties props) {
        var log = props.getLogging();
        var include = new HashSet<>(log.includePatterns());
        var exclude = new HashSet<>(log.excludePatterns());

        var filter = new RequestLoggingFilter(include, exclude, log.maxPayloadLength());
        var reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(0); // 가장 앞
        reg.addUrlPatterns("/*");

        return reg;
    }
}
