package org.example.order.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.order.client.kafka.autoconfig.KafkaAutoConfiguration;
import org.example.order.client.s3.autoconfig.S3AutoConfiguration;
import org.example.order.client.web.autoconfig.WebAutoConfiguration;
import org.example.order.common.support.json.ObjectMapperFactory;
import org.example.order.core.application.config.ApplicationAutoConfiguration;
import org.example.order.core.infra.common.idgen.tsid.config.TsidInfraConfig;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

@Configuration
@Import({
        OrderCoreConfig.class,
        WebAutoConfiguration.class,
        TsidInfraConfig.class
})
@ImportAutoConfiguration({
        S3AutoConfiguration.class,
        KafkaAutoConfiguration.class,
        CacheAutoConfiguration.class,
        ApplicationAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "org.example.order.batch.config",
        "org.example.order.batch.service",
        "org.example.order.batch.facade",
        "org.example.order.batch.job",
        "org.example.order.batch.application",
        "org.example.order.batch.exception",
        "org.example.order.batch.lifecycle",
        "org.example.order.batch.synchronize",
        "org.example.order.batch.mapper"
})
@EnableConfigurationProperties
@RequiredArgsConstructor
public class OrderBatchConfig {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return ObjectMapperFactory.defaultObjectMapper();
    }
}
