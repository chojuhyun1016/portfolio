package org.example.order.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.order.client.web.autoconfig.WebAutoConfiguration;
import org.example.order.common.support.json.ObjectMapperFactory;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.example.order.core.infra.common.idgen.tsid.config.TsidInfraConfig;
import org.example.order.worker.config.properties.AppCryptoKeyProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * OrderWorkerConfig
 * ------------------------------------------------------------------------
 * 목적
 * - 코어/웹 클라이언트/TSID 구성 임포트 + 워커 레이어 정밀 스캔.
 * - worker 모듈 전용 프로퍼티(AppCryptoKeyProperties) 바인딩 활성화.
 * <p>
 * [구성 요소]
 * - @Import: OrderCoreConfig, WebAutoConfiguration, TsidInfraConfig
 * - @ComponentScan: worker.*, core.application.order.mapper, client.kafka.*
 * - @EnableConfigurationProperties: AppCryptoKeyProperties
 */
@Configuration
@Import({
        OrderCoreConfig.class,
        WebAutoConfiguration.class,
        TsidInfraConfig.class
})
@ComponentScan(basePackages = {
        // 워커 애플리케이션 레이어
        "org.example.order.worker.config",
        "org.example.order.worker.service",
        "org.example.order.worker.facade",
        "org.example.order.worker.controller",
        "org.example.order.worker.listener",
        "org.example.order.worker.lifecycle",
        "org.example.order.worker.crypto",

        // MapStruct 매퍼 구현체
        "org.example.order.core.application.order.mapper",

        // Kafka 클라이언트(프로듀서/컨슈머 설정 및 서비스)
        "org.example.order.client.kafka.config",
        "org.example.order.client.kafka.service"
})
@EnableConfigurationProperties({
        AppCryptoKeyProperties.class
})
@RequiredArgsConstructor
public class OrderWorkerConfig {

    @org.springframework.context.annotation.Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return ObjectMapperFactory.defaultObjectMapper();
    }
}
