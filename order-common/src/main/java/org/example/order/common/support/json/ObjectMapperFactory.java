package org.example.order.common.support.json;

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
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

import static org.example.order.common.helper.datetime.DateTimeFormat.*;

/**
 * ObjectMapperFactory
 * ------------------------------------------------------------------------
 * - 공통 ObjectMapper 빌더.
 * - 계약 모듈(order-contract)과의 충돌을 피하기 위해 커스텀 enum 역/직렬화는 여기서 등록하지 않음.
 * - 날짜/시간은 고정 포맷(문자열)로 직렬화.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ObjectMapperFactory {

    public static ObjectMapper defaultObjectMapper() {
        return new Jackson2ObjectMapperBuilder()
                .failOnUnknownProperties(false)
                .featuresToDisable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
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
}
