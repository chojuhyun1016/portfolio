package org.example.order.api.common.config.module;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
public class LocalDateTimeParamBinder implements Converter<String, LocalDateTime> {

    private final DateTimeFormatter format;

    @Override
    public LocalDateTime convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(source, format);
    }
}
