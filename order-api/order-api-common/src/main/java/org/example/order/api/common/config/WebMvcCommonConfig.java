package org.example.order.api.common.config;

import java.util.List;

import org.example.order.common.web.AccessUserArgumentResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 공통 WebMvc 설정: AccessUserArgumentResolver 등록
 */
@AutoConfiguration
public class WebMvcCommonConfig implements WebMvcConfigurer {

    @Bean
    public AccessUserArgumentResolver accessUserArgumentResolver() {
        return new AccessUserArgumentResolver();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(accessUserArgumentResolver());
    }
}
