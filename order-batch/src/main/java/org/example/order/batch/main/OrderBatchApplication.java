package org.example.order.batch.main;

import jakarta.annotation.PostConstruct;
import org.example.order.core.config.FlywayDevLocalStrategy;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.example.order.batch.config.OrderBatchConfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * OrderBatchApplication
 * ------------------------------------------------------------------------
 * 목적
 * - 배치 잡 + 스케줄러(S3 동기화 등) + 라이프사이클 핸들러를 상시 구동하는 실행 엔트리.
 * 정렬
 * - worker 스타일로 동일하게 @EnableScheduling, @Import 구성 적용.
 * - 팀 컨벤션에 맞춰 main 패키지로 이동(스캔 루트 명확화).
 * 변경사항
 * - System.exit(...) 제거: 스케줄러/리스너 동작 보장.
 * - UTC 타임존 고정(운영 표준인 경우) — 필요 없으면 메서드 제거.
 * - RedisRepositoriesAutoConfiguration 제외는 기본 제거(필요 시 주석 해제).
 */
@SpringBootApplication(
        exclude = {RedisRepositoriesAutoConfiguration.class}
)
@EnableScheduling
@Import({
        OrderBatchConfig.class,
        FlywayDevLocalStrategy.class,
        OrderCoreConfig.class
})
public class OrderBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderBatchApplication.class, args);
    }

    @PostConstruct
    void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
