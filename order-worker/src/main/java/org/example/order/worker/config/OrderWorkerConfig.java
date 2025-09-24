package org.example.order.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.order.client.web.autoconfig.WebAutoConfiguration;
import org.example.order.common.support.json.ObjectMapperFactory;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * OrderWorkerConfig
 * ------------------------------------------------------------------------
 * 애플리케이션 구성의 중앙 진입점
 * <p>
 * 역할
 * - 코어 모듈 및 웹 클라이언트 자동 구성만 @Import
 * - 나머지는 @ComponentScan 으로 필요한 패키지 "정밀 스캔"
 * - 전역 스캔 금지(가독성/부팅시간/예상치 못한 빈 충돌 방지)
 * <p>
 * 포함/스캔 대상
 * - worker.* (service/facade/controller/listener/lifecycle/config)
 * - core.application.order.mapper (MapStruct 매퍼 구현체)
 * - client.kafka.* (KafkaTemplate, Producer/Consumer 설정/서비스)
 * <p>
 * 비고
 * - ObjectMapper 가 외부에서 주입되지 않은 경우에만 기본 ObjectMapper 제공
 */
@Configuration
@Import({
        OrderCoreConfig.class,
        WebAutoConfiguration.class
})
@ComponentScan(basePackages = {
        // 워커 애플리케이션 레이어
        "org.example.order.worker.config",
        "org.example.order.worker.service",
        "org.example.order.worker.facade",
        "org.example.order.worker.controller",
        "org.example.order.worker.listener",
        "org.example.order.worker.lifecycle",

        // MapStruct 매퍼 구현체
        "org.example.order.core.application.order.mapper",

        // Kafka 클라이언트(프로듀서/컨슈머 설정 및 서비스)
        "org.example.order.client.kafka.config",
        "org.example.order.client.kafka.service"
})
@RequiredArgsConstructor
public class OrderWorkerConfig {

    @org.springframework.context.annotation.Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return ObjectMapperFactory.defaultObjectMapper();
    }
}
