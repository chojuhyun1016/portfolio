package org.example.order.api.common.config.module;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.YearMonthSerializer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static org.example.order.common.format.DefaultDateTimeFormat.*;
import static org.example.order.common.format.DefaultDateTimeFormat.DATE_TIME_FORMAT;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FormatResourceFactory {

    @Getter
    private static final List<Converter<?, ?>> parameterBinders = List.of(new LocalDateTimeParamBinder(DATE_TIME_FORMAT));

    @Getter
    private static final List<ConverterFactory<?, ?>> parameterBinderFactory = List.of(new StringToEnumConverterFactory());

    @Getter
    private static final ObjectMapper requestObjectMapper = new Jackson2ObjectMapperBuilder()
            .failOnUnknownProperties(false) // SpringBoot default
            .featuresToDisable(MapperFeature.DEFAULT_VIEW_INCLUSION) // SpringBoot default
            .featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // SpringBoot default
            .serializerByType(LocalDate.class, new LocalDateSerializer(DATE_FORMAT))
            .deserializerByType(LocalDate.class, new LocalDateDeserializer(DATE_FORMAT))
            .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMAT))
            .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMAT))
            .serializerByType(LocalTime.class, new LocalTimeSerializer(TIME_FORMAT))
            .deserializerByType(LocalTime.class, new LocalTimeDeserializer(TIME_FORMAT))
            .serializerByType(YearMonth.class, new YearMonthSerializer(YEAR_MONTH_FORMAT))
            .deserializerByType(YearMonth.class, new YearMonthDeserializer(YEAR_MONTH_FORMAT))
            .build();
}
