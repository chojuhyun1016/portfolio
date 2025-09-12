package org.example.order.api.master.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.client.kafka.config.KafkaModuleConfig;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.common.support.json.ObjectMapperFactory;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
@Import({
        OrderCoreConfig.class,
        KafkaModuleConfig.class
})
@EnableConfigurationProperties(KafkaTopicProperties.class)
@ComponentScan(basePackages = {
        "org.example.order.api.master"
})
public class OrderApiMasterConfig {

    /**
     * 공통 ObjectMapper (없으면 기본 제공)
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return ObjectMapperFactory.defaultObjectMapper();
    }
}
