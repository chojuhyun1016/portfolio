package org.example.order.batch.main;

import jakarta.annotation.PostConstruct;
import org.example.order.core.config.FlywayDevLocalStrategy;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.example.order.batch.config.OrderBatchConfig;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import java.util.TimeZone;

/**
 * OrderBatchApplication
 * ------------------------------------------------------------------------
 * 목적
 * - 단발성 배치 실행 후 자연 종료.
 * 변경
 * - 웹 비활성화(WebApplicationType.NONE) -> Job 실행 후 컨텍스트 종료.
 * - 스케줄링 사용 안 함(스케줄러 관련 빈/코드 제거).
 */
@SpringBootApplication
@Import({
        OrderBatchConfig.class,
        FlywayDevLocalStrategy.class,
        OrderCoreConfig.class
})
public class OrderBatchApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(OrderBatchApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    @PostConstruct
    void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
