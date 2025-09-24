package org.example.order.client.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.order.client.kafka.autoconfig.KafkaAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmbeddedKafka 기반 통합 테스트
 * - 자동구성(KafkaAutoConfiguration)만 로드
 * - producer.enabled=true + 동적 bootstrap-servers 주입
 */
@SpringBootTest(
        properties = {
                "kafka.producer.enabled=true",
                "kafka.ssl.enabled=false"
        }
)
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@EmbeddedKafka(partitions = 1, controlledShutdown = false, topics = {KafkaProducerIT.TOPIC})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
class KafkaProducerIT {

    static final String TOPIC = "it-kafka-topic";

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static String resolveBrokers() {
        String brokers = System.getProperty("spring.embedded.kafka.brokers");
        return (brokers == null || brokers.isBlank()) ? "127.0.0.1:9092" : brokers;
    }

    @DynamicPropertySource
    static void registerKafkaBootstrap(DynamicPropertyRegistry registry) {
        // 자동구성의 조건을 만족시키는 프로퍼티 주입
        registry.add("kafka.producer.bootstrap-servers", KafkaProducerIT::resolveBrokers);
    }

    @Test
    @DisplayName("KafkaTemplate 전송 → Raw Consumer 폴링 수신 검증")
    void sendAndConsume() {
        String key = "it-key";
        String value = "hello-kafka-" + UUID.randomUUID();
        // JsonSerializer 사용 시 문자열은 따옴표가 감싸질 수 있어서 둘 다 허용
        String jsonEncodedValue = "\"" + value + "\"";

        kafkaTemplate.send(TOPIC, key, value).join();
        kafkaTemplate.flush();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-consumer-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        boolean matched = false;

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));
            long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();

            while (System.currentTimeMillis() < deadline && !matched) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> r : records.records(TOPIC)) {
                    String recKey = r.key();
                    String recVal = r.value();

                    if (key.equals(recKey) && (value.equals(recVal) || jsonEncodedValue.equals(recVal))) {
                        matched = true;
                        break;
                    }
                }
            }
        }

        assertTrue(matched, "Produced message must be consumed from topic: " + TOPIC);
    }
}
