package org.example.order.api.common.web.binder;

import org.springframework.core.convert.converter.Converter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 주입된 포맷으로 문자열을 날짜/시간 타입으로 변환하는 컨버터 제공.
 */
public class DateTimeBinder {

    private final DateTimeFormatter date;
    private final DateTimeFormatter time;
    private final DateTimeFormatter dateTime;
    private final DateTimeFormatter yearMonth;

    public DateTimeBinder(DateTimeFormatter date,
                          DateTimeFormatter time,
                          DateTimeFormatter dateTime,
                          DateTimeFormatter yearMonth) {
        this.date = date;
        this.time = time;
        this.dateTime = dateTime;
        this.yearMonth = yearMonth;
    }

    public Converter<String, LocalDate> localDateConverter() {
        return s -> LocalDate.parse(s, date);
    }

    public Converter<String, LocalTime> localTimeConverter() {
        return s -> LocalTime.parse(s, time);
    }

    public Converter<String, LocalDateTime> localDateTimeConverter() {
        return s -> LocalDateTime.parse(s, dateTime);
    }

    public Converter<String, YearMonth> yearMonthConverter() {
        return s -> YearMonth.parse(s, yearMonth);
    }
}
