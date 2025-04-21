package org.example.order.common.jackson.config;

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
import lombok.NoArgsConstructor;
import org.example.order.common.code.enums.CodeEnum;
import org.example.order.common.jackson.converter.CodeEnumJsonConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

import static org.example.order.common.format.DefaultDateTimeFormat.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonObjectMapperFactory {

    public static ObjectMapper defaultObjectMapper() {
        return new Jackson2ObjectMapperBuilder()
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
                .serializerByType(CodeEnum.class, new CodeEnumJsonConverter.Serializer())
                .deserializerByType(Enum.class, new CodeEnumJsonConverter.Deserializer())
                .build();
    }
}
