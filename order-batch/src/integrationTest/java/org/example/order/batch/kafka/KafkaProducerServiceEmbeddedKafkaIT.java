package org.example.order.batch.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.order.batch.IntegrationBoot;
import org.example.order.batch.kafka.support.EmbeddedKafkaITBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Embedded Kafka 통합 테스트.
 * - KafkaTemplate로 발행 → 컨슈머로 수신 검증
 */
@SpringBootTest(
        classes = IntegrationBoot.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.main.web-application-type=none"
)
@DirtiesContext
class KafkaProducerServiceEmbeddedKafkaIT extends EmbeddedKafkaITBase {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void ORDER_LOCAL_메시지_발행_수신() {
        String topic = "ORDER_LOCAL";
        String key = "k1";
        String payload = "hello-order-batch";

        kafkaTemplate.send(topic, key, payload);
        kafkaTemplate.flush();

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getProperty("spring.embedded.kafka.brokers"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-consumer-group-batch");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (var consumer = new DefaultKafkaConsumerFactory<String, String>(props).createConsumer()) {
            consumer.subscribe(java.util.List.of(topic));

            var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

            assertThat(records.isEmpty()).isFalse();
            var record = records.records(topic).iterator().next();
            assertThat(record.key()).isEqualTo(key);
            assertThat(record.value()).isEqualTo(payload);
        }
    }
}
