package org.example.order.client.kafka;

import org.example.order.client.kafka.config.KafkaModuleConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 큰 맥락
 * - Kafka Producer 설정이 활성화(producer.enabled=true) 되었을 때
 * KafkaTemplate 빈이 생성되는지 확인하는 단위 테스트.
 * - bootstrap-servers 값은 더미(localhost:9092)로 설정,
 * 실제 브로커 연결 시도는 하지 않고 단순 빈 존재 여부만 검증한다.
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
    @DisplayName("producer.enabled=true → KafkaTemplate 빈 생성")
    void kafkaTemplateBeanPresentWhenEnabled() {
        KafkaTemplate<String, Object> template = ctx.getBean(KafkaTemplate.class);
        assertNotNull(template, "KafkaTemplate must be created when enabled=true");
    }
}
