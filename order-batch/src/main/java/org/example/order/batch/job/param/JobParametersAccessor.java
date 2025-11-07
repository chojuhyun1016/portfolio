package org.example.order.batch.job.param;

import lombok.Getter;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * JobParametersAccessor
 * ------------------------------------------------------------------------
 * 목적
 * - 잡 파라미터(version, s3Key, date, yearMonth, dateTime)를 안전하게 노출하는 공용 접근자.
 * - @JobScope 로 잡 인스턴스마다 생성.
 * <p>
 * 변경사항
 * - 기존 CustomJobParameter 를 job.param 패키지로 이동/개명.
 * - Lombok @Getter 전역 사용 시 getDate()/getYearMonth() 중복/혼동을 피하기 위해
 * 원본 문자열 필드는 getXxxParam() 형태로 노출하고, 파싱 메서드는 parseXxx()로 분리.
 * - [추가] dateTime(시분초까지)의 원본/파싱 지원을 추가 (겹침 실행 방지 및 상세 시간 인자 수용).
 * 기존 date/yearMonth 사용 범위는 유지하며, 단지 시분초까지 입력을 받을 수 있게 확장.
 */
@JobScope
@Component
public class JobParametersAccessor {

    /**
     * 원본 파라미터 문자열들
     */
    @Getter
    @Value("#{jobParameters[version]}")
    private String version;

    @Getter
    @Value("#{jobParameters[s3Key]}")
    private String s3Key;

    @Value("#{jobParameters[date]}")
    private String dateParam;

    @Value("#{jobParameters[yearMonth]}")
    private String yearMonthParam;

    @Value("#{jobParameters[dateTime]}")
    private String dateTimeParam;

    /**
     * 원본 문자열 접근자 (메서드 이름 충돌 방지)
     */
    public Optional<String> getDateParam() {
        return Optional.ofNullable(dateParam);
    }

    public Optional<String> getYearMonthParam() {
        return Optional.ofNullable(yearMonthParam);
    }

    /**
     * dateTime 원본 접근자
     */
    public Optional<String> getDateTimeParam() {
        return Optional.ofNullable(dateTimeParam);
    }

    /**
     * 파싱된 타입 반환 (필요 시만 사용)
     */
    public Optional<LocalDate> parseDate() {
        return getDateParam().map(LocalDate::parse);
    }

    public Optional<YearMonth> parseYearMonth() {
        return getYearMonthParam().map(YearMonth::parse);
    }

    /**
     * - 지원 포맷 우선순위:
     * 1) ISO_LOCAL_DATE_TIME: yyyy-MM-dd'T'HH:mm:ss
     * 2) 공백 구분:          yyyy-MM-dd HH:mm:ss
     * 3) 압축형:             yyyyMMddHHmmss
     * - 파라미터가 없거나 파싱 실패 시 Optional.empty()
     * - 기존 배치와의 호환을 위해, date만 주어진 경우까지 자동 변환하지는 않습니다(명시적 사용).
     */
    public Optional<LocalDateTime> parseDateTime() {
        return getDateTimeParam().flatMap(JobParametersAccessor::tryParseDateTimeFlexible);
    }

    private static Optional<LocalDateTime> tryParseDateTimeFlexible(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (DateTimeParseException ignore) {
        }

        try {
            DateTimeFormatter sp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            return Optional.of(LocalDateTime.parse(raw, sp));
        } catch (DateTimeParseException ignore) {
        }

        try {
            DateTimeFormatter compact = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

            return Optional.of(LocalDateTime.parse(raw, compact));
        } catch (DateTimeParseException ignore) {
        }

        return Optional.empty();
    }
}
