package org.example.order.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.order.client.kafka.autoconfig.KafkaAutoConfiguration;
import org.example.order.client.s3.autoconfig.S3AutoConfiguration;
import org.example.order.client.web.autoconfig.WebAutoConfiguration;
import org.example.order.common.support.json.ObjectMapperFactory;
import org.example.order.core.infra.common.idgen.tsid.config.TsidInfraConfig;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.example.order.worker.config.properties.AppCryptoKeyProperties;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.example.order.core.application.config.ApplicationAutoConfiguration;

/**
 * OrderWorkerConfig
 * ------------------------------------------------------------------------
 * 목적
 * - 코어/웹 클라이언트/TSID 구성 임포트 + 워커 레이어 정밀 스캔.
 * - worker 모듈 전용 프로퍼티(AppCryptoKeyProperties) 바인딩 활성화.
 * <p>
 * [구성 요소]
 * - @Import: OrderCoreConfig, WebAutoConfiguration, TsidInfraConfig
 * - @ImportAutoConfiguration: S3AutoConfiguration, KafkaAutoConfiguration, ApplicationAutoConfiguration
 * - @ComponentScan: worker.*
 * - @EnableConfigurationProperties: AppCryptoKeyProperties
 */
@Configuration
@Import({
        OrderCoreConfig.class,
        WebAutoConfiguration.class,
        TsidInfraConfig.class
})
@ImportAutoConfiguration({
        S3AutoConfiguration.class,
        KafkaAutoConfiguration.class,
        ApplicationAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "org.example.order.worker.config",
        "org.example.order.worker.service",
        "org.example.order.worker.facade",
        "org.example.order.worker.listener",
        "org.example.order.worker.lifecycle",
        "org.example.order.worker.crypto",
        "org.example.order.worker.mapper"
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
