package org.example.order.batch.application;

import lombok.Getter;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

@Getter
@JobScope
@Component
public class CustomJobParameter {

    @Value("#{jobParameters[version]}")
    private String version;
    @Value("#{jobParameters[s3Key]}")
    private String s3Key;
    @Value("#{jobParameters[date]}")
    private String date;
    @Value("#{jobParameters[yearMonth]}")
    private String yearMonth;

    public LocalDate getDate() {
        return LocalDate.parse(date);
    }

    public YearMonth getYearMonth() {
        return YearMonth.parse(yearMonth);
    }
}
