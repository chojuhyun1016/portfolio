package org.example.order.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.common.support.json.ObjectMapperFactory;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Order Batch 구성 루트
 * - core/infra만 @Import
 * - kafka/s3는 각 모듈의 AutoConfiguration에 맡김(프로퍼티 스위치로 on/off)
 * - Batch 모듈 자체 패키지만 ComponentScan (외부 모듈은 스캔하지 않음)
 * - BatchProperties 바인딩 활성화
 */
@Configuration
@Import({
        OrderCoreConfig.class
})
@EnableConfigurationProperties(BatchProperties.class)
@ComponentScan(basePackages = {
        "org.example.order.batch.config",
        "org.example.order.batch.application",
        "org.example.order.batch.facade",
        "org.example.order.batch.job",
        "org.example.order.batch.service"
})
public class OrderBatchConfig {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return ObjectMapperFactory.defaultObjectMapper();
    }
}
