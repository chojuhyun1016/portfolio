package org.example.order.common.utils.datetime;

import lombok.experimental.UtilityClass;
import org.example.order.common.code.type.RegionCode;
import org.example.order.common.code.type.ZoneCode;
import org.example.order.common.code.type.ZoneMapper;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * 날짜 및 시간 관련 유틸리티 클래스
 * - 지역/시간대 간 변환
 * - 현재 시각 및 날짜 가져오기
 * - 날짜 범위 계산
 * - Epoch 밀리초 및 마이크로초 변환
 */
@UtilityClass
public class DateTimeUtils {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM");

    // ============================================================
    // ▶ 시간대 변환 메서드
    // ============================================================

    /**
     * 지정된 ZoneCode 간 LocalDateTime 변환
     *
     * @param dateTime 변환할 시간
     * @param from     원본 ZoneCode
     * @param to       대상 ZoneCode
     * @return 변환된 LocalDateTime (to 기준) - null 방어
     */
    public static LocalDateTime convertBetweenZones(LocalDateTime dateTime, ZoneCode from, ZoneCode to) {
        if (dateTime == null || from == null || to == null)  {
            return null;
        }

        return dateTime.atZone(from.getZoneId())
                .withZoneSameInstant(to.getZoneId())
                .toLocalDateTime();
    }

    /**
     * 지정된 RegionCode 간 LocalDateTime 변환
     *
     * @param dateTime 변환할 시간
     * @param from     원본 RegionCode
     * @param to       대상 RegionCode
     * @return 변환된 LocalDateTime (to 기준) - null 방어
     */
    public static LocalDateTime convertBetweenRegions(LocalDateTime dateTime, RegionCode from, RegionCode to) {
        if (dateTime == null || from == null || to == null) {
            return null;
        }

        return dateTime.atZone(resolveZoneId(from))
                .withZoneSameInstant(resolveZoneId(to))
                .toLocalDateTime();
    }

    // ============================================================
    // ▶ Epoch 변환 메서드
    // ============================================================

    /**
     * Epoch 마이크로초(Long)을 LocalDateTime으로 변환
     *
     * @param timestamp Epoch 타임스탬프 (마이크로초 또는 밀리초)
     * @return 변환된 LocalDateTime
     */
    public static LocalDateTime longToLocalDateTime(long timestamp) {
        Instant instant;
        boolean isMicroseconds = String.valueOf(timestamp).length() > 13;

        if (isMicroseconds) {
            long epochSeconds = timestamp / 1_000_000;
            long microAdjustment = (timestamp % 1_000_000) * 1_000;
            instant = Instant.ofEpochSecond(epochSeconds, microAdjustment);
        } else {
            instant = Instant.ofEpochMilli(timestamp);
        }

        return LocalDateTime.ofInstant(instant, ZoneCode.UTC.getZoneId());
    }

    /**
     * LocalDateTime을 Epoch 마이크로초(Long)로 변환
     *
     * @param localDateTime 변환할 LocalDateTime
     * @return Epoch 마이크로초
     */
    public static Long localDateTimeToLong(LocalDateTime localDateTime) {
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
        return (instant.getEpochSecond() * 1_000_000) + (instant.getNano() / 1_000);
    }

    // ============================================================
    // ▶ 현재 시각 및 날짜 메서드
    // ============================================================

    /**
     * 현재 시각 (기본 UTC 기준)
     */
    public static LocalDateTime getCurrentDateTime() {
        return getCurrentDateTime(ZoneCode.UTC);
    }

    /**
     * 현재 시각 (ZoneCode 기준)
     */
    public static LocalDateTime getCurrentDateTime(ZoneCode zone) {
        return ZonedDateTime.now(resolveZoneId(zone)).toLocalDateTime();
    }

    /**
     * 현재 시각 (RegionCode 기준)
     */
    public static LocalDateTime getCurrentDateTime(RegionCode region) {
        return ZonedDateTime.now(resolveZoneId(region)).toLocalDateTime();
    }

    /**
     * 현재 날짜 (기본 UTC 기준)
     */
    public static LocalDate getCurrentDate() {
        return getCurrentDate(ZoneCode.UTC);
    }

    /**
     * 현재 날짜 (ZoneCode 기준)
     */
    public static LocalDate getCurrentDate(ZoneCode zone) {
        return ZonedDateTime.now(resolveZoneId(zone)).toLocalDate();
    }

    /**
     * 현재 날짜 (RegionCode 기준)
     */
    public static LocalDate getCurrentDate(RegionCode region) {
        return ZonedDateTime.now(resolveZoneId(region)).toLocalDate();
    }

    // ============================================================
    // ▶ Epoch 밀리초 메서드
    // ============================================================

    /**
     * 현재 시간의 Epoch 밀리초 (기본 UTC 기준)
     */
    public static long nowTime() {
        return nowTime(ZoneCode.UTC);
    }

    /**
     * 현재 시간의 Epoch 밀리초 (ZoneCode 기준)
     */
    public static long nowTime(ZoneCode zone) {
        return ZonedDateTime.now(resolveZoneId(zone)).toInstant().toEpochMilli();
    }

    /**
     * 현재 시간의 Epoch 밀리초 (RegionCode 기준)
     */
    public static long nowTime(RegionCode region) {
        return ZonedDateTime.now(resolveZoneId(region)).toInstant().toEpochMilli();
    }

    // ============================================================
    // ▶ 월 값 포맷 메서드
    // ============================================================

    /**
     * LocalDateTime 기준 MM 문자열 반환
     */
    public static String getMonthValueAsString(LocalDateTime localDateTime) {
        return localDateTime.format(MONTH_FORMATTER);
    }

    /**
     * YearMonth 기준 MM 문자열 반환
     */
    public static String getMonthValueAsString(YearMonth yearMonth) {
        return yearMonth.format(MONTH_FORMATTER);
    }

    // ============================================================
    // ▶ 날짜 범위 계산 메서드
    // ============================================================

    /**
     * 하루의 시작 시각 (00:00:00)
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    /**
     * 하루의 종료 시각 (23:59:59.999999999)
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    /**
     * 월의 시작일 00:00:00
     */
    public static LocalDateTime startOfMonth(LocalDateTime date) {
        return YearMonth.from(date).atDay(1).atStartOfDay();
    }

    /**
     * 월의 종료일 23:59:59.000
     */
    public static LocalDateTime endOfMonth(LocalDateTime date) {
        return YearMonth.from(date).atEndOfMonth().atTime(23, 59, 59, 0);
    }

    // ============================================================
    // ▶ 내부 ZoneId 변환 메서드
    // ============================================================

    /**
     * ZoneCode에서 ZoneId 추출 (null-safe)
     */
    private static ZoneId resolveZoneId(ZoneCode zone) {
        return zone != null ? zone.getZoneId() : ZoneCode.UTC.getZoneId();
    }

    /**
     * RegionCode에서 ZoneId 추출 (null-safe)
     */
    private static ZoneId resolveZoneId(RegionCode region) {
        return ZoneMapper.zoneOf(region).getZoneId();
    }
}
