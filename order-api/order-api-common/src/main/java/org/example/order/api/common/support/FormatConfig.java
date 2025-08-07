package org.example.order.api.common.support;

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

import static org.example.order.common.helper.datetime.DateTimeFormat.*;

/**
 * 파라미터 변환기 및 ObjectMapper 생성 팩토리
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FormatConfig {

    // RequestParam용 바인딩 컨버터
    @Getter
    private static final List<Converter<?, ?>> parameterBinders = List.of(
            new DateTimeBinder(DATE_TIME_FORMAT)
    );

    // enum 바인딩용 컨버터 팩토리
    @Getter
    private static final List<ConverterFactory<?, ?>> parameterBinderFactory = List.of(
            new EnumBinder()
    );

    // 날짜 형식을 지정한 ObjectMapper
    @Getter
    private static final ObjectMapper requestObjectMapper = new Jackson2ObjectMapperBuilder()
            .failOnUnknownProperties(false)
            .featuresToDisable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
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
