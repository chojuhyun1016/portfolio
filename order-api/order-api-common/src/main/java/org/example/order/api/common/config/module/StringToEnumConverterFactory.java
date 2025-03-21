package org.example.order.api.common.config.module;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

@SuppressWarnings({"rawtypes", "unchecked"})
public class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {

    @Override
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToEnum(getEnumType(targetType));
    }

    private static class StringToEnum<T extends Enum> implements Converter<String, T> {

        private final Class<T> enumType;

        StringToEnum(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        @Nullable
        public T convert(String source) {
            if (source.isEmpty() || "ALL".equals(source)) {
                return null;
            }
            return (T) Enum.valueOf(this.enumType, source.trim());
        }
    }

    private static Class<?> getEnumType(Class<?> targetType) {
        Class enumType;
        for (enumType = targetType; enumType != null && !enumType.isEnum(); enumType = enumType.getSuperclass()) {

        }
        Assert.notNull(enumType, () -> {
            return "The target type " + targetType.getName() + " does not refer to an enum";
        });
        return enumType;
    }
}
