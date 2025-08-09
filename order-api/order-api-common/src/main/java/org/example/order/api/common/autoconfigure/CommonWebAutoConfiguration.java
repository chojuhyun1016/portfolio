package org.example.order.api.common.autoconfigure;

import org.example.order.api.common.infra.ApiInfraProperties;
import org.example.order.api.common.web.advice.GlobalExceptionHandler;
import org.example.order.api.common.web.binder.DateTimeBinder;
import org.example.order.api.common.web.binder.EnumBinder;
import org.example.order.api.common.web.config.WebMvcCommonConfig;
import org.example.order.api.common.web.filter.RequestLoggingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * 공통 Web 자동 구성:
 * - 전역 예외 핸들러
 * - 바인더/포맷터 등록
 * - 요청 로깅 필터 등록
 */
@AutoConfiguration
@EnableConfigurationProperties(ApiInfraProperties.class)
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public DateTimeBinder dateTimeBinder() {
        return new DateTimeBinder(
                org.example.order.common.helper.datetime.DateTimeFormat.DATE_FORMAT,
                org.example.order.common.helper.datetime.DateTimeFormat.TIME_FORMAT,
                org.example.order.common.helper.datetime.DateTimeFormat.DATE_TIME_FORMAT,
                org.example.order.common.helper.datetime.DateTimeFormat.YEAR_MONTH_FORMAT
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public EnumBinder enumBinder() {
        return new EnumBinder();
    }

    @Bean
    @ConditionalOnMissingBean
    public WebMvcCommonConfig webMvcCommonConfig(DateTimeBinder dateTimeBinder, EnumBinder enumBinder) {
        return new WebMvcCommonConfig(dateTimeBinder, enumBinder);
    }

    @Bean(name = "requestLoggingFilterRegistration")
    @ConditionalOnMissingBean(name = "requestLoggingFilterRegistration")
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilterRegistration(ApiInfraProperties props) {
        FilterRegistrationBean<RequestLoggingFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new RequestLoggingFilter(props));
        reg.setOrder(props.getLogging().getFilterOrder());
        reg.addUrlPatterns("/*");
        return reg;
    }
}
