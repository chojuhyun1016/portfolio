package org.example.order.common.helper.datetime;

import lombok.experimental.UtilityClass;
import org.example.order.common.core.code.type.RegionCode;
import org.example.order.common.core.code.type.ZoneCode;
import org.example.order.common.helper.zone.ZoneMapper;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * 날짜 및 시간 유틸리티 클래스.
 * <p>
 * - 시간대 변환 (ZoneCode, RegionCode)
 * - 현재 시각 및 날짜 조회
 * - Epoch 밀리초 변환 및 역변환
 * - 월 포맷 값 반환
 * - 날짜 범위 계산 (하루/월 단위)
 */
@UtilityClass
public class DateTimeUtils {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM");

    // ==============================
    // 시간대 변환
    // ==============================

    public static LocalDateTime convertBetweenZones(LocalDateTime dateTime, ZoneCode from, ZoneCode to) {
        if (dateTime == null || from == null || to == null) {
            return null;
        }

        return dateTime.atZone(from.getZoneId())
                .withZoneSameInstant(to.getZoneId())
                .toLocalDateTime();
    }

    public static LocalDateTime convertBetweenRegions(LocalDateTime dateTime, RegionCode from, RegionCode to) {
        if (dateTime == null || from == null || to == null) {
            return null;
        }

        ZoneId fromZone = ZoneMapper.zoneOf(from).getZoneId();
        ZoneId toZone = ZoneMapper.zoneOf(to).getZoneId();

        return dateTime.atZone(fromZone)
                .withZoneSameInstant(toZone)
                .toLocalDateTime();
    }

    // ==============================
    // 현재 시각
    // ==============================

    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }

    public static LocalDateTime getCurrentDateTime(ZoneCode zone) {
        ZoneId zoneId = zone != null ? zone.getZoneId() : ZoneId.systemDefault();
        return ZonedDateTime.now(zoneId).toLocalDateTime();
    }

    public static LocalDateTime getCurrentDateTime(RegionCode region) {
        ZoneId zoneId = ZoneMapper.zoneOf(region).getZoneId();
        return ZonedDateTime.now(zoneId).toLocalDateTime();
    }

    // ==============================
    // 현재 날짜
    // ==============================

    public static LocalDate getCurrentDate() {
        return LocalDate.now();
    }

    public static LocalDate getCurrentDate(ZoneCode zone) {
        ZoneId zoneId = zone != null ? zone.getZoneId() : ZoneId.systemDefault();
        return ZonedDateTime.now(zoneId).toLocalDate();
    }

    public static LocalDate getCurrentDate(RegionCode region) {
        ZoneId zoneId = ZoneMapper.zoneOf(region).getZoneId();
        return ZonedDateTime.now(zoneId).toLocalDate();
    }

    // ==============================
    // 에포크 밀리초
    // ==============================

    public static long nowTime() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static long nowTime(ZoneCode zone) {
        ZoneId zoneId = zone != null ? zone.getZoneId() : ZoneId.systemDefault();
        return ZonedDateTime.now(zoneId).toInstant().toEpochMilli();
    }

    public static long nowTime(RegionCode region) {
        ZoneId zoneId = ZoneMapper.zoneOf(region).getZoneId();
        return ZonedDateTime.now(zoneId).toInstant().toEpochMilli();
    }

    // ==============================
    // Epoch 변환 (밀리초 ↔ LocalDateTime)
    // ==============================

    public static LocalDateTime longToLocalDateTime(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }

        return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static Long localDateTimeToLong(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }

        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // ==============================
    // 월 포맷 반환
    // ==============================

    public static String getMonthValueAsString(LocalDateTime localDateTime) {
        return localDateTime.format(MONTH_FORMATTER);
    }

    public static String getMonthValueAsString(YearMonth yearMonth) {
        return yearMonth.format(MONTH_FORMATTER);
    }

    // ==============================
    // 날짜 범위 계산
    // ==============================

    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    public static LocalDateTime atEndOfDay(LocalDate date) {
        return date.atTime(23, 59, 59, 0);
    }

    public static LocalDateTime startOfMonth(LocalDateTime date) {
        return YearMonth.from(date).atDay(1).atStartOfDay();
    }

    public static LocalDateTime endOfMonth(LocalDateTime date) {
        return YearMonth.from(date).atEndOfMonth().atTime(23, 59, 59, 0);
    }
}
