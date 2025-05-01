package org.example.order.common.datetime;

import lombok.experimental.UtilityClass;
import org.example.order.common.code.type.RegionCode;
import org.example.order.common.code.type.ZoneCode;
import org.example.order.common.code.type.ZoneMapper;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * 날짜 및 시간 관련 통합 유틸리티 클래스
 *
 * 기능:
 * - 시간대 변환 (ZoneCode, RegionCode)
 * - LocalDateTime ↔ Epoch (밀리초, 마이크로초)
 * - 현재 시각 및 날짜 반환
 * - 날짜 범위 계산 (start/end of day, month 등)
 * - 문자열 ↔ LocalDate/LocalDateTime/YearMonth 변환 (포맷 포함)
 *
 * 기본 시간대: UTC
 */
@UtilityClass
public class DateTimeUtils {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM");

    // ============================================================
    // ▶ 시간대 변환 메서드
    // ============================================================

    /**
     * ZoneCode 간 LocalDateTime 변환
     */
    public static LocalDateTime convertBetweenZones(LocalDateTime dateTime, ZoneCode from, ZoneCode to) {
        if (dateTime == null || from == null || to == null) {
            return null;
        }
        return dateTime.atZone(from.getZoneId())
                .withZoneSameInstant(to.getZoneId())
                .toLocalDateTime();
    }

    /**
     * RegionCode 간 LocalDateTime 변환
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
     * Epoch 마이크로초(Long) → LocalDateTime (UTC)
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

        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /**
     * LocalDateTime → Epoch 마이크로초(Long) (UTC)
     */
    public static Long localDateTimeToMicroseconds(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        Instant instant = localDateTime.atZone(ZoneOffset.UTC).toInstant();
        return (instant.getEpochSecond() * 1_000_000) + (instant.getNano() / 1_000);
    }

    /**
     * LocalDateTime → Epoch 밀리초(Long) (UTC)
     */
    public static Long localDateTimeToMilliseconds(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    // ============================================================
    // ▶ 현재 시각 및 날짜 메서드
    // ============================================================

    public static LocalDateTime getCurrentDateTime() {
        return ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime();
    }

    public static LocalDateTime getCurrentDateTime(ZoneCode zone) {
        return ZonedDateTime.now(resolveZoneId(zone)).toLocalDateTime();
    }

    public static LocalDateTime getCurrentDateTime(RegionCode region) {
        return ZonedDateTime.now(resolveZoneId(region)).toLocalDateTime();
    }

    public static LocalDate getCurrentDate() {
        return ZonedDateTime.now(ZoneOffset.UTC).toLocalDate();
    }

    public static LocalDate getCurrentDate(ZoneCode zone) {
        return ZonedDateTime.now(resolveZoneId(zone)).toLocalDate();
    }

    public static LocalDate getCurrentDate(RegionCode region) {
        return ZonedDateTime.now(resolveZoneId(region)).toLocalDate();
    }

    // ============================================================
    // ▶ Epoch 밀리초 메서드
    // ============================================================

    public static long nowTime() {
        return ZonedDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    public static long nowTime(ZoneCode zone) {
        return ZonedDateTime.now(resolveZoneId(zone)).toInstant().toEpochMilli();
    }

    public static long nowTime(RegionCode region) {
        return ZonedDateTime.now(resolveZoneId(region)).toInstant().toEpochMilli();
    }

    // ============================================================
    // ▶ 월 값 포맷 메서드
    // ============================================================

    public static String getMonthValueAsString(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.format(MONTH_FORMATTER) : null;
    }

    public static String getMonthValueAsString(YearMonth yearMonth) {
        return yearMonth != null ? yearMonth.format(MONTH_FORMATTER) : null;
    }

    // ============================================================
    // ▶ 날짜 범위 계산 메서드
    // ============================================================

    public static LocalDateTime startOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    public static LocalDateTime endOfDay(LocalDate date) {
        return date != null ? date.atTime(LocalTime.MAX) : null;
    }

    public static LocalDateTime startOfMonth(LocalDateTime date) {
        return date != null ? YearMonth.from(date).atDay(1).atStartOfDay() : null;
    }

    public static LocalDateTime endOfMonth(LocalDateTime date) {
        return date != null ? YearMonth.from(date).atEndOfMonth().atTime(23, 59, 59, 0) : null;
    }

    // ============================================================
    // ▶ 문자열 ↔ LocalDate/Time 변환 (포맷)
    // ============================================================

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormat.DATE_FORMAT) : null;
    }

    public static LocalDate parseDate(String date) {
        return (date != null && !date.isEmpty()) ? LocalDate.parse(date, DateTimeFormat.DATE_FORMAT) : null;
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormat.DATE_TIME_FORMAT) : null;
    }

    public static LocalDateTime parseDateTime(String dateTime) {
        return (dateTime != null && !dateTime.isEmpty()) ? LocalDateTime.parse(dateTime, DateTimeFormat.DATE_TIME_FORMAT) : null;
    }

    public static String formatNoneDashDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormat.DATE_NONE_DASH_FORMAT) : null;
    }

    public static LocalDate parseNoneDashDate(String date) {
        return (date != null && !date.isEmpty()) ? LocalDate.parse(date, DateTimeFormat.DATE_NONE_DASH_FORMAT) : null;
    }

    public static String formatNoneDashDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormat.DATE_TIME_NONE_DASH_FORMAT) : null;
    }

    public static LocalDateTime parseNoneDashDateTime(String dateTime) {
        return (dateTime != null && !dateTime.isEmpty()) ? LocalDateTime.parse(dateTime, DateTimeFormat.DATE_TIME_NONE_DASH_FORMAT) : null;
    }

    public static String formatYearMonth(YearMonth yearMonth) {
        return yearMonth != null ? yearMonth.format(DateTimeFormat.YEAR_MONTH_FORMAT) : null;
    }

    public static YearMonth parseYearMonth(String yearMonth) {
        return (yearMonth != null && !yearMonth.isEmpty()) ? YearMonth.parse(yearMonth, DateTimeFormat.YEAR_MONTH_FORMAT) : null;
    }

    public static String formatNoneDashYearMonth(YearMonth yearMonth) {
        return yearMonth != null ? yearMonth.format(DateTimeFormat.YEAR_MONTH_NONE_DASH_FORMAT) : null;
    }

    public static YearMonth parseNoneDashYearMonth(String yearMonth) {
        return (yearMonth != null && !yearMonth.isEmpty()) ? YearMonth.parse(yearMonth, DateTimeFormat.YEAR_MONTH_NONE_DASH_FORMAT) : null;
    }

    // ============================================================
    // ▶ 내부 ZoneId 변환 메서드
    // ============================================================

    private static ZoneId resolveZoneId(ZoneCode zone) {
        return zone != null ? zone.getZoneId() : ZoneOffset.UTC;
    }

    private static ZoneId resolveZoneId(RegionCode region) {
        return ZoneMapper.zoneOf(region).getZoneId();
    }
}
