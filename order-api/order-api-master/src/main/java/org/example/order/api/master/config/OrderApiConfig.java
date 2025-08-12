package org.example.order.api.master.config;

import org.example.order.client.kafka.config.KafkaModuleConfig;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 공통 모듈의 AutoConfiguration을 신뢰합니다.
 * 로컬 커스터마이징이 필요할 때만 설정을 추가하세요.
 */
@Configuration
@Import({OrderCoreConfig.class, KafkaModuleConfig.class})
@EnableConfigurationProperties(KafkaTopicProperties.class)
@ComponentScan({
        "org.example.order.client.kafka",    // client-kafka 빈 스캔
        "org.example.order.api.master"       // 현재 모듈
})
public class OrderApiConfig {
}
