package org.example.order.batch.job.param;

import lombok.Getter;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

/**
 * JobParametersAccessor
 * ------------------------------------------------------------------------
 * 목적
 * - 잡 파라미터(version, s3Key, date, yearMonth)를 안전하게 노출하는 공용 접근자.
 * - @JobScope 로 잡 인스턴스마다 생성.
 * <p>
 * 변경사항
 * - 기존 CustomJobParameter 를 job.param 패키지로 이동/개명.
 * - Lombok @Getter 전역 사용 시 getDate()/getYearMonth() 중복/혼동을 피하기 위해
 * 원본 문자열 필드는 getXxxParam() 형태로 노출하고, 파싱 메서드는 parseXxx()로 분리.
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
     * 파싱된 타입 반환 (필요 시만 사용)
     */
    public Optional<LocalDate> parseDate() {
        return getDateParam().map(LocalDate::parse);
    }

    public Optional<YearMonth> parseYearMonth() {
        return getYearMonthParam().map(YearMonth::parse);
    }
}
