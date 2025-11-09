package org.example.order.common.support.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.YearMonthSerializer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;

import static org.example.order.common.helper.datetime.DateTimeFormat.*;

/**
 * ObjectMapperFactory
 * ------------------------------------------------------------------------
 * - 공통 ObjectMapper 빌더.
 * - 계약 모듈(order-contract)과의 충돌을 피하기 위해 커스텀 enum 역/직렬화는 여기서 등록하지 않음.
 * - 날짜/시간은 고정 포맷(문자열)로 직렬화.
 * <p>
 * 보강 사항(역직렬화만 관대화, 직렬화 포맷 불변)
 * - LocalDateTime 역직렬화 시 기존 고정 포맷(DATE_TIME_FORMAT) 외에 다음 포맷도 허용:
 * 1) ISO_LOCAL_DATE_TIME (예: 2025-09-21T06:00:00)
 * 2) 유연 포맷: yyyy-MM-dd['T'][' ']HH:mm[:ss][.SSS][.SS][.S]
 * - Serializer(출력 포맷)는 그대로 유지 -> 기존 서비스/계약에 영향 없음
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ObjectMapperFactory {

    public static ObjectMapper defaultObjectMapper() {
        return new Jackson2ObjectMapperBuilder()
                .failOnUnknownProperties(false)
                .featuresToDisable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .serializerByType(LocalDate.class, new LocalDateSerializer(DATE_FORMAT))
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMAT))
                .serializerByType(LocalTime.class, new LocalTimeSerializer(TIME_FORMAT))
                .serializerByType(YearMonth.class, new YearMonthSerializer(YEAR_MONTH_FORMAT))
                .deserializerByType(LocalDate.class, new LocalDateDeserializer(DATE_FORMAT))
                .deserializerByType(LocalTime.class, new LocalTimeDeserializer(TIME_FORMAT))
                .deserializerByType(YearMonth.class, new YearMonthDeserializer(YEAR_MONTH_FORMAT))
                .deserializerByType(LocalDateTime.class, new LenientLocalDateTimeDeserializer(DATE_TIME_FORMAT))
                .build();
    }

    /**
     * LocalDateTime 관대 역직렬화기
     * - 1순위: 기존 고정 포맷(DATE_TIME_FORMAT) 시도
     * - 2순위: ISO_LOCAL_DATE_TIME (예: 2025-09-21T06:00:00)
     * - 3순위: 유연 포맷 yyyy-MM-dd['T'][' ']HH:mm[:ss][.SSS][.SS][.S]
     * - 위 모두 실패 시, Jackson 표준 방식으로 에러 위임
     * <p>
     * 주의: Serializer는 그대로 두므로 출력(JSON)은 기존과 동일 포맷을 유지함
     */
    private static final class LenientLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

        private static final DateTimeFormatter FLEXIBLE = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .optionalStart().appendLiteral('T').optionalEnd()
                .optionalStart().appendLiteral(' ').optionalEnd()
                .appendPattern("HH:mm")
                .optionalStart().appendPattern(":ss").optionalEnd()
                .optionalStart().appendPattern(".SSS").optionalEnd()
                .optionalStart().appendPattern(".SS").optionalEnd()
                .optionalStart().appendPattern(".S").optionalEnd()
                .toFormatter();

        private final List<DateTimeFormatter> candidates;

        private LenientLocalDateTimeDeserializer(DateTimeFormatter primary) {
            super(LocalDateTime.class);

            this.candidates = List.of(
                    primary,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                    FLEXIBLE
            );
        }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final String raw = p.getValueAsString();

            if (raw == null || raw.isBlank()) {
                return null;
            }

            final String s = raw.trim();

            for (DateTimeFormatter f : candidates) {
                try {
                    return LocalDateTime.parse(s, f);
                } catch (Exception ignore) {
                }
            }

            try {
                return LocalDateTime.parse(s);
            } catch (Exception ignore) {
            }

            return (LocalDateTime) ctxt.handleWeirdStringValue(
                    handledType(),
                    s,
                    "Unsupported LocalDateTime format. tried primary/ISO/flexible patterns"
            );
        }
    }
}
