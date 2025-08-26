package org.example.order.client.kafka;

import org.example.order.client.kafka.config.KafkaModuleConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 큰 맥락
 * - Kafka Producer 설정의 조건부 빈 생성 동작을 검증하는 단위 테스트.
 * - producer.enabled=false 일 때는 KafkaTemplate 빈이 생성되지 않아야 한다.
 * - ApplicationContext 에서 KafkaTemplate 조회 시 NoSuchBeanDefinitionException 발생을 확인한다.
 */
@SpringBootTest(classes = KafkaModuleConfig.class)
@TestPropertySource(properties = {
        "kafka.producer.enabled=false" // 프로듀서 비활성화
})
class KafkaProducerConfigDisabledTest {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationContext ctx;

    @Test
    @DisplayName("producer.enabled=false → KafkaTemplate 빈 미생성")
    void kafkaTemplateBeanAbsentWhenDisabled() {
        assertThrows(
                NoSuchBeanDefinitionException.class,
                () -> ctx.getBean(org.springframework.kafka.core.KafkaTemplate.class)
        );
    }
}
