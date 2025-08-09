package org.example.order.api.common.autoconfigure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.YearMonthSerializer;
import org.example.order.api.common.infra.ApiInfraProperties;
import org.example.order.api.common.web.binder.DateTimeBinder;
import org.example.order.api.common.web.binder.EnumBinder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import static org.example.order.common.helper.datetime.DateTimeFormat.*;

/**
 * 포맷 자동 구성:
 * - Date/Time 컨버터 등록
 * - Enum ConverterFactory 등록
 * - Jackson 포맷 커스터마이즈(문자열 포맷 기본, 필요시 timestamp 스위치)
 */
@AutoConfiguration
@EnableConfigurationProperties(ApiInfraProperties.class)
public class CommonFormatAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DateTimeBinder dateTimeBinder() {
        return new DateTimeBinder(DATE_FORMAT, TIME_FORMAT, DATE_TIME_FORMAT, YEAR_MONTH_FORMAT);
    }

    @Bean
    @ConditionalOnMissingBean
    public EnumBinder enumBinder() {
        return new EnumBinder();
    }

    @Bean
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilderCustomizer dateTimeAndEnumCustomizer(ApiInfraProperties props) {
        return builder -> {
            builder.featuresToDisable(MapperFeature.DEFAULT_VIEW_INCLUSION);
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            if (props.getFormat().isWriteDatesAsTimestamps()) {
                builder.featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            } else {
                builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }

            builder.serializers(
                    new LocalDateSerializer(DATE_FORMAT),
                    new LocalTimeSerializer(TIME_FORMAT),
                    new LocalDateTimeSerializer(DATE_TIME_FORMAT),
                    new YearMonthSerializer(YEAR_MONTH_FORMAT)
            );
            builder.deserializers(
                    new LocalDateDeserializer(DATE_FORMAT),
                    new LocalTimeDeserializer(TIME_FORMAT),
                    new LocalDateTimeDeserializer(DATE_TIME_FORMAT),
                    new YearMonthDeserializer(YEAR_MONTH_FORMAT)
            );
        };
    }
}
