package org.example.order.api.common.support;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * RequestParam(String) → LocalDateTime 바인딩용 컨버터
 */
@RequiredArgsConstructor
public class DateTimeBinder implements Converter<String, LocalDateTime> {

    private final DateTimeFormatter format;

    @Override
    public LocalDateTime convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }

        return LocalDateTime.parse(source, format);
    }
}
