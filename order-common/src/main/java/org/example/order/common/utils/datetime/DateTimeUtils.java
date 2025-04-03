package org.example.order.common.utils.datetime;

import lombok.experimental.UtilityClass;

import java.time.*;
import static org.example.order.common.constant.DateTimeConstant.*;

@UtilityClass
public class DateTimeUtils {

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

        return LocalDateTime.ofInstant(instant, ZoneId.of(UTC));
    }

    // return micro seconds
    public static Long localDateTimeToLong(LocalDateTime localDateTime) {
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
        return (instant.getEpochSecond() * 1_000_000) + (instant.getNano() / 1_000);
    }

    public static Long nowTime() {
        return LocalDateTime.now().atZone(ZoneId.of(UTC)).toInstant().toEpochMilli();
    }

    public static LocalDateTime startOfDay(final LocalDate date) {
        return date.atStartOfDay();
    }

    public static LocalDateTime endOfDay(final LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    public static LocalDateTime startOfMonth(final LocalDateTime date) {
        return startOfMonth(YearMonth.from(date));
    }

    public static LocalDateTime startOfMonth(final YearMonth yearMonth) {
        return yearMonth.atDay(1).atStartOfDay();
    }

    public static LocalDateTime endOfMonth(final LocalDateTime date) {
        return endOfMonth(YearMonth.from(date));
    }

    public static LocalDateTime endOfMonth(final YearMonth yearMonth) {
        return yearMonth.atEndOfMonth().atTime(23 ,59, 59, 0);
    }
}