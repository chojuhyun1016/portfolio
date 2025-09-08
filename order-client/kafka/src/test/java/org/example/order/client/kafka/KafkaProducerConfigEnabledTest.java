package org.example.order.client.kafka;

import org.example.order.client.kafka.config.KafkaModuleConfig;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = KafkaModuleConfig.class)
@TestPropertySource(properties = {
        "kafka.producer.enabled=true",
        "kafka.producer.bootstrap-servers=localhost:9092",
        "kafka.ssl.enabled=false"
})
class KafkaProducerConfigEnabledTest {

    @Autowired
    private KafkaTemplate<String, Object> template;

    @Autowired
    private KafkaProducerCluster cluster;

    @Test
    @DisplayName("producer.enabled=true → KafkaTemplate/KafkaProducerCluster 생성")
    void kafkaBeansPresentWhenEnabled() {
        assertNotNull(template, "KafkaTemplate must be created when enabled=true");
        assertNotNull(cluster, "KafkaProducerCluster must be created when enabled=true");
    }
}
