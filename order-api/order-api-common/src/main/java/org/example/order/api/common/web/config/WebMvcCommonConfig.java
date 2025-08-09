package org.example.order.api.common.web.config;

import org.example.order.api.common.web.binder.DateTimeBinder;
import org.example.order.api.common.web.binder.EnumBinder;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 공통 WebMvc 설정: 바인더(컨버터) 등록.
 * - 실제 등록은 Auto-Configuration에서 이 클래스를 빈으로 만들어 적용
 */
public class WebMvcCommonConfig implements WebMvcConfigurer {

    private final DateTimeBinder dateTimeBinder;
    private final EnumBinder enumBinder;

    public WebMvcCommonConfig(DateTimeBinder dateTimeBinder, EnumBinder enumBinder) {
        this.dateTimeBinder = dateTimeBinder;
        this.enumBinder = enumBinder;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(dateTimeBinder.localDateConverter());
        registry.addConverter(dateTimeBinder.localTimeConverter());
        registry.addConverter(dateTimeBinder.localDateTimeConverter());
        registry.addConverter(dateTimeBinder.yearMonthConverter());
        registry.addConverterFactory(enumBinder);
    }
}
