package org.example.order.api.common.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * RequestParam(String) → Enum<T> 자동 바인딩
 * - 빈 문자열, "ALL"은 null 처리
 */
public class EnumBinder implements ConverterFactory<String, Enum> {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToEnum<>(targetType);
    }

    private static class StringToEnum<T extends Enum<T>> implements Converter<String, T> {
        private final Class<T> enumType;

        public StringToEnum(Class<T> enumType) {
            this.enumType = resolveEnumType(enumType);
        }

        @Override
        @Nullable
        public T convert(String source) {
            if (source == null || source.isBlank() || "ALL".equalsIgnoreCase(source)) {
                return null;
            }

            return Enum.valueOf(enumType, source.trim());
        }

        @SuppressWarnings("unchecked")
        private static <T extends Enum<T>> Class<T> resolveEnumType(Class<?> type) {
            Class<?> enumType = type;

            while (enumType != null && !enumType.isEnum()) {
                enumType = enumType.getSuperclass();
            }

            Assert.notNull(enumType, () -> "Type " + type.getName() + " is not an enum");

            return (Class<T>) enumType;
        }
    }
}
