package org.example.order.client.kafka;

import org.example.order.client.kafka.config.KafkaModuleConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * producer.enabled=true 이고 bootstrap-servers 가 지정되면
 * - KafkaTemplate 생성
 * - KafkaProducerCluster 생성
 * (실제 브로커 연결 시도는 하지 않음)
 */
@SpringBootTest(classes = KafkaModuleConfig.class)
@TestPropertySource(properties = {
        "kafka.producer.enabled=true",                     // 프로듀서 활성화
        "kafka.producer.bootstrap-servers=localhost:9092", // 더미 주소
        "kafka.ssl.enabled=false"                          // SSL 비활성화
})
class KafkaProducerConfigEnabledTest {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationContext ctx;

    @Test
    @DisplayName("producer.enabled=true → KafkaTemplate/KafkaProducerCluster 생성")
    void kafkaBeansPresentWhenEnabled() {
        KafkaTemplate<String, Object> template = ctx.getBean(KafkaTemplate.class);

        assertNotNull(template, "KafkaTemplate must be created when enabled=true");

        org.example.order.client.kafka.service.KafkaProducerCluster cluster =
                ctx.getBean(org.example.order.client.kafka.service.KafkaProducerCluster.class);

        assertNotNull(cluster, "KafkaProducerCluster must be created when enabled=true");
    }
}
