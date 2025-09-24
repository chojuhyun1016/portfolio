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
 * - 모듈(레이어) 오염 방지: core 타입(CodeEnum, DlqType) 직접 참조/등록하지 않음.
 * - enum 직렬화/역직렬화는
 * 1) @JsonComponent(CodeEnumJsonConverter) 의 자동 등록(Serializer) +
 * 2) 각 enum 개별 @JsonDeserialize(using=...) 로 처리(Deserializer)
 * - DlqType 역직렬화는 필드에 붙인 @JsonDeserialize(DlqTypeStringDeserializer) 로 처리.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ObjectMapperFactory {

    public static ObjectMapper defaultObjectMapper() {
        return new Jackson2ObjectMapperBuilder()
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
}
