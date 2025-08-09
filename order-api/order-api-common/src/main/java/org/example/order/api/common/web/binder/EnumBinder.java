package org.example.order.api.common.web.binder;

import org.springframework.core.convert.converter.ConverterFactory;

/**
 * "Foo" / " foo  " / "FOO" → Foo 로 변환하는 범용 Enum 컨버터 팩토리.
 */
public class EnumBinder implements ConverterFactory<String, Enum> {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <T extends Enum> org.springframework.core.convert.converter.Converter<String, T> getConverter(Class<T> targetType) {
        return source -> {
            String normalized = source.trim();

            if (normalized.isEmpty()) {
                return null;
            }

            return (T) Enum.valueOf(targetType, normalized.toUpperCase());
        };
    }
}
