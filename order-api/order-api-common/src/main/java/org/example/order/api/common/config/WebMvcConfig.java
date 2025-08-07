package org.example.order.api.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.api.common.interceptor.AccessUserInterceptor;
import org.example.order.api.common.resolver.AccessUserArgumentResolver;
import org.example.order.api.common.support.FormatConfig;
import org.example.order.api.common.support.EnumBinder;
import org.example.order.common.support.json.ObjectMapperFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;

/**
 * Spring WebMvc 설정 구성
 */
@Configuration
@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 정적 리소스 핸들링 설정
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(
                        "classpath:/META-INF/resources/",
                        "classpath:/resources/",
                        "classpath:/static/",
                        "classpath:/public/");
    }

    /**
     * 인터셉터 등록
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AccessUserInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/docs/**", "/favicon.ico", "/actuator/**", "/error");
    }

    /**
     * 컨트롤러 메서드 파라미터 리졸버 등록
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AccessUserArgumentResolver());
    }

    /**
     * 커스텀 Converter 및 ConverterFactory 등록
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // LocalDateTime, Enum용 커스텀 컨버터 등록
        FormatConfig.getParameterBinderFactory().forEach(registry::addConverterFactory);
        FormatConfig.getParameterBinders().forEach(registry::addConverter);

        // 명시적으로 Enum 변환기 등록 (중복 등록 가능)
        registry.addConverterFactory(new EnumBinder());
    }

    /**
     * Jackson 기반 메시지 컨버터 등록
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter(objectMapper()));
        converters.add(new StringHttpMessageConverter());
        converters.add(new ResourceHttpMessageConverter());
    }

    /**
     * Jackson ObjectMapper Bean 등록
     */
    @Bean
    public ObjectMapper objectMapper() {
        return ObjectMapperFactory.defaultObjectMapper();
    }
}
