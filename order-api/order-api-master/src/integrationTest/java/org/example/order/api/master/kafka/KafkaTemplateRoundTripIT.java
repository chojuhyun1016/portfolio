package org.example.order.api.master.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.order.api.master.IntegrationBoot;
import org.example.order.api.master.kafka.support.EmbeddedKafkaITBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EmbeddedKafka 라운드트립
 * - 테스트 내부에서 Producer/Consumer 직접 생성
 * - Redis/Redisson 자동설정 제외(V2)
 */
@SpringBootTest(
        classes = IntegrationBoot.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.main.web-application-type=none"
)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
                "org.redisson.spring.starter.RedissonAutoConfigurationV2"
})
@DirtiesContext
class KafkaTemplateRoundTripIT extends EmbeddedKafkaITBase {

    @Test
    @DisplayName("EmbeddedKafka: 수동 Producer 발행 → 수동 Consumer 수신")
    void ORDER_LOCAL_publish_and_consume() {
        String brokers = System.getProperty("spring.embedded.kafka.brokers");
        String topic = "ORDER_LOCAL";
        String key = "k1";
        String payload = "hello-order";

        // ----- Producer (수동 생성)
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        KafkaTemplate<String, String> kafkaTemplate =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));

        kafkaTemplate.send(topic, key, payload);
        kafkaTemplate.flush();

        // ----- Consumer (수동 생성)
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "it-consumer-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (var consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer()) {
            consumer.subscribe(java.util.List.of(topic));
            var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

            assertThat(records.isEmpty()).isFalse();
            var record = records.records(topic).iterator().next();
            assertThat(record.key()).isEqualTo(key);
            assertThat(record.value()).isEqualTo(payload);
        }
    }
}
